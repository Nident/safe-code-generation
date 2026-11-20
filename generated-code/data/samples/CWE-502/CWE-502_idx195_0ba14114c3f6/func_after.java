@Override
    public void collect(CollectRep.MetricsData.Builder builder, long appId, String app, Metrics metrics) {
        long startTime = System.currentTimeMillis();
        // 校验参数
        try {
            validateParams(metrics);
        } catch (Exception e) {
            builder.setCode(CollectRep.Code.FAIL);
            builder.setMsg(e.getMessage());
            return;
        }
        SnmpProtocol snmpProtocol = metrics.getSnmp();
        int timeout = CollectUtil.getTimeout(snmpProtocol.getTimeout());
        int snmpVersion = getSnmpVersion(snmpProtocol.getVersion());
        try {
            SnmpBuilder snmpBuilder = new SnmpBuilder();
            Snmp snmpService = getSnmpService(snmpVersion);
            Target<?> target;
            Address targetAddress = GenericAddress.parse(DEFAULT_PROTOCOL + ":" + snmpProtocol.getHost()
                    + "/" + snmpProtocol.getPort());
            TargetBuilder<?> targetBuilder = snmpBuilder.target(targetAddress);
            if (snmpVersion == SnmpConstants.version3) {
                target = targetBuilder
                        .user(snmpProtocol.getUsername())
                        .auth(TargetBuilder.AuthProtocol.hmac192sha256).authPassphrase(snmpProtocol.getAuthPassphrase())
                        .priv(TargetBuilder.PrivProtocol.aes128).privPassphrase(snmpProtocol.getPrivPassphrase())
                        .done()
                        .timeout(timeout).retries(1)
                        .build();
            } else if (snmpVersion == SnmpConstants.version1) {
                target = targetBuilder
                        .v1()
                        .community(new OctetString(snmpProtocol.getCommunity()))
                        .timeout(timeout).retries(1)
                        .build();
                target.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv1);
            } else {
                target = targetBuilder
                        .v2c()
                        .community(new OctetString(snmpProtocol.getCommunity()))
                        .timeout(timeout).retries(1)
                        .build();
                target.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv2c);
            }

            PDU pdu = targetBuilder.pdu().type(PDU.GET).oids(snmpProtocol.getOids().values().toArray(new String[0])).build();
            SnmpCompletableFuture snmpRequestFuture = SnmpCompletableFuture.send(snmpService, target, pdu);
            List<VariableBinding> vbs = snmpRequestFuture.get().getAll();
            long responseTime = System.currentTimeMillis() - startTime;
            Map<String, String> oidsMap = snmpProtocol.getOids();
            Map<String, String> oidsValueMap = new HashMap<>(oidsMap.size());
            for (VariableBinding binding : vbs) {
                Variable variable = binding.getVariable();
                if (variable instanceof TimeTicks) {
                    String value = ((TimeTicks) variable).toString(FORMAT_PATTERN);
                    oidsValueMap.put(binding.getOid().toDottedString(), value);
                } else {
                    oidsValueMap.put(binding.getOid().toDottedString(), binding.toValueString());
                }
            }
            CollectRep.ValueRow.Builder valueRowBuilder = CollectRep.ValueRow.newBuilder();
            for (String alias : metrics.getAliasFields()) {
                if (CollectorConstants.RESPONSE_TIME.equalsIgnoreCase(alias)) {
                    valueRowBuilder.addColumns(Long.toString(responseTime));
                } else {
                    String oid = oidsMap.get(alias);
                    String value = oidsValueMap.get(oid);
                    if (value != null) {
                        valueRowBuilder.addColumns(value);
                    } else {
                        valueRowBuilder.addColumns(CommonConstants.NULL_VALUE);
                    }
                }
            }
            builder.addValues(valueRowBuilder.build());
        } catch (ExecutionException | InterruptedException ex) {
            log.info("[snmp collect] error: {}", ex.getMessage());
            builder.setCode(CollectRep.Code.UN_CONNECTABLE);
            builder.setMsg(ex.getMessage());
        } catch (Exception e) {
            log.warn("[snmp collect] error: {}", e.getMessage(), e);
            builder.setCode(CollectRep.Code.FAIL);
            builder.setMsg(e.getMessage());
        }
    }