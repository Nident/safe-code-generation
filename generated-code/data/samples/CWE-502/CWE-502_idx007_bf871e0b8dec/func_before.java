private FlowRegistryClientEntity populateRemainingRegistryClientEntityContent(final FlowRegistryClientEntity flowRegistryClientEntity) {
        flowRegistryClientEntity.setUri(generateResourceUri("controller", "registry-clients", flowRegistryClientEntity.getId()));

        if (flowRegistryClientEntity.getComponent().getType().equals(NIFI_REGISTRY_TYPE)) {
            flowRegistryClientEntity.getComponent().setUri(flowRegistryClientEntity.getComponent().getProperties().get("url"));
        }

        return flowRegistryClientEntity;
    }