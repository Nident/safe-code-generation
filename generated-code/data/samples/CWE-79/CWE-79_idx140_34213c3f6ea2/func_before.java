@Handler(supports = { AppointmentType.class }, order = 50)
public class AppointmentTypeValidator implements Validator {
	
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@Autowired
	@Qualifier("appointmentService")
	private AppointmentService appointmentService;
	
	
	@SuppressWarnings("unchecked")
	public boolean supports(Class c) {
		return c.equals(AppointmentType.class);
	}
	
	public void setAppointmentService(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}
	
	public AppointmentService getAppointmentService() {
		return appointmentService;
	}
	
	
	public void validate(Object obj, Errors errors) {
		AppointmentType appointmentType = (AppointmentType) obj;
		if (appointmentType == null) {
			errors.rejectValue("appointmentType", "error.general");
		} else {
			validateDurationField(errors, appointmentType);
			validateFieldName(errors, appointmentType);
			validateDescriptionField(errors, appointmentType.getDescription());
		}
	}
	
	private void validateFieldName(Errors errors, AppointmentType appointmentType) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "error.name");
		if (appointmentService.verifyDuplicatedAppointmentTypeName(appointmentType)) {
			errors.rejectValue("name", "appointmentscheduling.AppointmentType.nameDuplicated");
		}
		if (verifyIfNameHasMoreThan100Characters(appointmentType.getName())) {
			errors.rejectValue("name", "appointmentscheduling.AppointmentType.longName.errorMessage");
		}
	}
	
	private boolean verifyIfNameHasMoreThan100Characters(String appointmentName) {
		if (appointmentName != null) {
			return (appointmentName.length() > 100) ? true : false;
		}
		return false;
	}
	
	private void validateDurationField(Errors errors, AppointmentType appointmentType) {
		ValidationUtils.rejectIfEmpty(errors, "duration", "appointmentscheduling.AppointmentType.durationEmpty");
		if (appointmentType.getDuration() == null || appointmentType.getDuration() <= 0) {
			errors.rejectValue("duration", "appointmentscheduling.AppointmentType.duration.errorMessage");
		}
	}
	
	private void validateDescriptionField(Errors errors, String description) {
		if (verifyIfDescriptionHasMoreThan1024Characters(description)) {
			errors.rejectValue("description", "appointmentscheduling.AppointmentType.description.errorMessage");
		}
	}
	
	private boolean verifyIfDescriptionHasMoreThan1024Characters(String description) {
		if (description != null) {
			return (description.length() > 1024) ? true : false;
		}
		return false;
	}
	
}