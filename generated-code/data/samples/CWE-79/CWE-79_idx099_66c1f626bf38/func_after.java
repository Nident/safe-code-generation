@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NotificationsPageIT extends OpenNMSSeleniumIT {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationsPageIT.class);

    @Before
    public void setUp() throws Exception {
        notificationsPage();
    }

    @Test
    public void testAllTextIsPresent() throws Exception {
        assertEquals(3, countElementsMatchingCss("div.card-header"));
        findElementByXpath("
        findElementByXpath("
        findElementByXpath("
    }

    @Test
    public void testAllLinksArePresent() {
        findElementByLink("Your outstanding notices");
        findElementByLink("All outstanding notices");
        findElementByLink("All acknowledged notices");
    }

    @Test
    public void testAllFormsArePresent() {
        findElementByXpath("
        findElementByXpath("
    }

    @Test
    public void testAllLinks() {
        findElementByLink("Your outstanding notices").click();
        findElementByXpath("
        findElementByLink("[Remove all]");
        findElementByLink("Sent Time");
        findElementByXpath("

        notificationsPage();
        findElementByLink("All outstanding notices").click();
        findElementByXpath("
        findElementByLink("[Show acknowledged]");
        findElementByLink("Respond Time");
        assertElementDoesNotHaveText(By.xpath("

        notificationsPage();
        findElementByLink("All acknowledged notices").click();
        findElementByXpath("
        findElementByLink("[Show outstanding]");
        findElementByLink("Respond Time");
        assertElementDoesNotHaveText(By.xpath("
    }
}