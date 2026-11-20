public class VersionedYamlDoc extends MappingNode {

	public VersionedYamlDoc(MappingNode wrapped) {
		super(wrapped.getTag(), wrapped.getValue(), wrapped.getFlowStyle());
	}
	
	public static VersionedYamlDoc fromYaml(String yaml) {
		return new VersionedYamlDoc((MappingNode) new OneYaml().compose(new StringReader(yaml)));
	}
	
	@SuppressWarnings("unchecked")
	public <T> T toBean(Class<T> beanClass) {
        setTag(new Tag(beanClass));
		if (getVersion() != null) {
			try {
				MigrationHelper.migrate(getVersion(), beanClass.newInstance(), this);
				removeVersion();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		
        return (T) new OneYaml().construct(this);
	}
	
	public static VersionedYamlDoc fromBean(Object bean) {
		VersionedYamlDoc doc = new VersionedYamlDoc((MappingNode) new OneYaml().represent(bean));
		doc.setVersion(MigrationHelper.getVersion(HibernateProxyHelper.getClassWithoutInitializingProxy(bean)));
		return doc;
	}
	
	private String getVersion() {
		for (NodeTuple tuple: getValue()) {
			ScalarNode keyNode = (ScalarNode) tuple.getKeyNode();
			if (keyNode.getValue().equals("version")) 
				return ((ScalarNode)tuple.getValueNode()).getValue();
		}
		throw new GeneralException("Unable to find version");
	}
	
	private void removeVersion() {
		for (Iterator<NodeTuple> it = getValue().iterator(); it.hasNext();) {
			ScalarNode keyNode = (ScalarNode) it.next().getKeyNode();
			if (keyNode.getValue().equals("version")) 
				it.remove();
		}
	}
	
	private void setVersion(String version) {
		ScalarNode versionNode = null;
		for (NodeTuple tuple:  getValue()) {
			ScalarNode keyNode = (ScalarNode) tuple.getKeyNode();
			if (keyNode.getValue().equals("version")) {
				((ScalarNode) tuple.getValueNode()).setValue(version);
				break;
			}
		}
		if (versionNode == null) {
			ScalarNode keyNode = new ScalarNode(Tag.STR, "version", null, null, DumperOptions.ScalarStyle.PLAIN);
			versionNode = new ScalarNode(Tag.INT, version, null, null, DumperOptions.ScalarStyle.PLAIN);
			getValue().add(0, new NodeTuple(keyNode, versionNode));
		}
	}
	
	public String toYaml() {
		StringWriter writer = new StringWriter();
		DumperOptions dumperOptions = new DumperOptions();
		Serializer serializer = new Serializer(new Emitter(writer, dumperOptions), 
				new Resolver(), dumperOptions, Tag.MAP);
		try {
			serializer.open();
			serializer.serialize(this);
			serializer.close();
			return writer.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static class OneConstructor extends Constructor {
		
		public Object construct(Node node) {
			return constructDocument(node);
		}

		@Override
		protected Class<?> getClassForNode(Node node) {
			if (node instanceof VersionedYamlDoc) {
				return super.getClassForNode(node);
			} else {
				Class<?> type = node.getType();
				if (type.getAnnotation(Editable.class) == null) {
					
					throw new IllegalStateException(String.format("Unexpected yaml node (type: %s, tag: %s)", 
							type, node.getTag()));
				} else {
					if (!ClassUtils.isConcrete(type)) {
						ImplementationRegistry registry = OneDev.getInstance(ImplementationRegistry.class);
						for (Class<?> implementationClass: registry.getImplementations(node.getType())) {
							String implementationTag = new Tag("!" + implementationClass.getSimpleName()).getValue();
							if (implementationTag.equals(node.getTag().getValue()))
								return implementationClass;
						}
					}
					return super.getClassForNode(node);
				}
			}
		}
		
	}
	
	private static class OneYaml extends Yaml {

		OneYaml() {
			super(newConstructor(), newRepresenter());
			
			
			setBeanAccess(BeanAccess.PROPERTY);
		}
		
		private static Representer newRepresenter() {
			Representer representer = new Representer() {
				
			    @SuppressWarnings("rawtypes")
				@Override
			    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, 
			    		Object propertyValue,Tag customTag) {
			        if (propertyValue == null 
			        		|| propertyValue instanceof Collection && ((Collection) propertyValue).isEmpty()
			        		|| propertyValue instanceof Map && ((Map) propertyValue).isEmpty()) { 
			        	return null;
			        } else {
			        	return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
			        }
			    }

			};
			representer.setDefaultFlowStyle(FlowStyle.BLOCK);
			representer.setPropertyUtils(new PropertyUtils() {

				@Override
				protected Set<Property> createPropertySet(Class<? extends Object> type, BeanAccess bAccess) {
					List<Property> properties = new ArrayList<>();
					Map<String, Integer> orders = new HashMap<>();
					if (type.getAnnotation(Editable.class) != null) {
						for (Method getter: BeanUtils.findGetters(type)) {
							Editable editable = getter.getAnnotation(Editable.class);
							Method setter = BeanUtils.findSetter(getter);
							if (editable != null && setter != null) {
								String propertyName = BeanUtils.getPropertyName(getter);
								try {
									properties.add(new MethodProperty(new PropertyDescriptor(propertyName, getter, setter)));
								} catch (IntrospectionException e) {
									throw new RuntimeException(e);
								}
								orders.put(propertyName, editable.order());
							}
						}
					}
					Collections.sort(properties, new Comparator<Property>() {

						@Override
						public int compare(Property o1, Property o2) {
							return orders.get(o1.getName()) - orders.get(o2.getName());
						}
						
					});
					return new LinkedHashSet<>(properties);
				}
				
			});
			return representer;
		}
		
		private static OneConstructor newConstructor() {
			return new OneConstructor();
		}
		
		public Object construct(Node node) {
	        return ((OneConstructor)constructor).construct(node);
		}
	    
	}

}