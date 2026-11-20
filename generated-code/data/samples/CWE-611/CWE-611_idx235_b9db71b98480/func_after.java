public Response revokeCert(CertId id, CertRevokeRequest request, boolean caCert) {
        if (id == null) {
            logger.warn("Unable to revoke cert: Missing certificate ID");
            throw new BadRequestException("Unable to revoke cert: Missing certificate ID");
        }
        if (request == null) {
            logger.warn("revokeCert: request is null");
            throw new BadRequestException("Unable to revoke cert: invalid request");
        }

        // check cert actually exists.  This will throw a CertNotFoundException
        // if the cert does not exist
        @SuppressWarnings("unused")
        CertData data = getCertData(id);

        RevocationReason revReason = request.getReason();
        if (revReason == RevocationReason.REMOVE_FROM_CRL) {
            return unrevokeCert(id);
        }

        String caIssuerDN = null;
        X500Name caX500DN = null;
        RevocationProcessor processor;
        try {
            processor = new RevocationProcessor("caDoRevoke-agent", getLocale(headers));
            processor.setStartTime(new Date().getTime());

            // TODO: set initiative based on auth info
            processor.setInitiative(AuditFormat.FROMAGENT);

            processor.setSerialNumber(id);

            processor.setRevocationReason(revReason);
            processor.setRequestType(revReason == RevocationReason.CERTIFICATE_HOLD
                    ? RevocationProcessor.ON_HOLD : RevocationProcessor.REVOKE);
            processor.setInvalidityDate(request.getInvalidityDate());
            processor.setComments(request.getComments());

            processor.setAuthority(authority);

            caX500DN = (X500Name) authority.getCACert().getSubjectDN();

        } catch (EBaseException e) {
            logger.error("Unable to revoke certificate: " + e.getMessage(), e);
            throw new PKIException("Unable to revoke certificate: " + e.getMessage(), e);
        }

        try {
            X509Certificate clientCert = null;
            try {
                clientCert = CAProcessor.getSSLClientCertificate(servletRequest);
            } catch (EBaseException e) {
                // No client certificate, ignore.
            }

            CertRecord clientRecord = null;
            BigInteger clientSerialNumber = null;
            String clientSubjectDN = null;

            if (clientCert != null) {
                clientSerialNumber = clientCert.getSerialNumber();
                clientSubjectDN = clientCert.getSubjectDN().toString();

                X500Name x500issuerDN = (X500Name) clientCert.getIssuerDN();
                /*
                 * internal revocation check only to be conducted for certs
                 * issued by this CA
                 * For client certs issued by external CAs, TLS mutual auth
                 * would have completed the authenticaton/verification if
                 * OCSP was enabled;
                 * Furthermore, prior to the actual revocation, client cert
                 * is mapped against the agent group database for proper
                 * privilege regardless of the issuer.
                 */
                if (x500issuerDN.equals(caX500DN)) {
                    logger.info("CertService.revokeCert: client cert issued by this CA");
                    clientRecord = processor.getCertificateRecord(clientSerialNumber);

                    // Verify client cert is not revoked.
                    // TODO: This should be checked during authentication.
                    if (clientRecord.getStatus().equals(CertRecord.STATUS_REVOKED)) {
                        throw new UnauthorizedException(CMS.getLogMessage("CMSGW_UNAUTHORIZED"));
                    }
                } else {
                    logger.info("CertService.revokeCert: client cert not issued by this CA");
                    if (!authority.allowExtCASignedAgentCerts()) {
                        logger.error("CertService.revokeCert: allowExtCASignedAgentCerts false;");
                        throw new UnauthorizedException(CMS.getLogMessage("CMSGW_UNAUTHORIZED"));
                    } else {
                        logger.info("CertService.revokeCert: allowExtCASignedAgentCerts true;");
                    }
                }
            }

            if (authority.noncesEnabled() &&
                !processor.isMemberOfSubsystemGroup(clientCert)) {
                processor.validateNonce(servletRequest, "cert-revoke", id.toBigInteger(), request.getNonce());

            }

            // Find target cert record if different from client cert.
            CertRecord targetRecord = id.equals(clientSerialNumber) ? clientRecord : processor.getCertificateRecord(id);
            X509CertImpl targetCert = targetRecord.getCertificate();

            processor.createCRLExtension();

            // TODO remove hardcoded role names and consult authzmgr
            // (so that we can handle externally-authenticated principals)
            GenericPrincipal principal =
                (GenericPrincipal) servletRequest.getUserPrincipal();
            String subjectDN = principal.hasRole("Certificate Manager Agents") ?
                    null : clientSubjectDN;

            processor.validateCertificateToRevoke(subjectDN, targetRecord, caCert);
            processor.addCertificateToRevoke(targetCert);
            processor.createRevocationRequest();

            processor.auditChangeRequest(ILogger.SUCCESS);

        } catch (PKIException e) {
            logger.warn("Unable to pre-process revocation request: " + e.getMessage());
            processor.auditChangeRequest(ILogger.FAILURE);
            throw e;

        } catch (EBaseException e) {
            logger.error("Unable to pre-process revocation request: " + e.getMessage(), e);
            processor.auditChangeRequest(ILogger.FAILURE);
            throw new PKIException("Unable to revoke cert: " + e.getMessage(), e);

        } catch (IOException e) {
            logger.error("Unable to pre-process revocation request: " + e.getMessage(), e);
            processor.auditChangeRequest(ILogger.FAILURE);
            throw new PKIException("Unable to revoke cert: " + e.getMessage(), e);
        }

        // change audit processing from "REQUEST" to "REQUEST_PROCESSED"
        // to distinguish which type of signed audit log message to save
        // as a failure outcome in case an exception occurs

        try {
            processor.processRevocationRequest();

            processor.auditChangeRequestProcessed(ILogger.SUCCESS);

        } catch (EBaseException e) {
            logger.error("Unable to process revocation request: " + e.getMessage(), e);
            processor.auditChangeRequestProcessed(ILogger.FAILURE);
            throw new PKIException("Unable to revoke certificate: " + e.getMessage(), e);
        }

        try {
            IRequest certRequest = processor.getRequest();
            CertRequestDAO dao = new CertRequestDAO();
            CertRequestInfo requestInfo = dao.getRequest(certRequest.getRequestId(), uriInfo);
            return createOKResponse(requestInfo);

        } catch (EBaseException e) {
            logger.error("Unable to create revocation response: " + e.getMessage(), e);
            throw new PKIException("Unable to create revocation response: " + e.getMessage(), e);
        }
    }