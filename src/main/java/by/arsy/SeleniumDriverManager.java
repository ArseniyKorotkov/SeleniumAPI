package by.arsy;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v126.network.Network;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.*;

public class SeleniumDriverManager {

	private static final String USER_AGENT_SCRIPT = "Mozilla/5.0 (%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s.0.0.0 Safari/537.36";
	private static final String USER_AGENT_DEFAULT_VERSION = "115";
	private final Duration waitDurationSeconds;
	private final String userAgentVersion;
	private ChromeDriver chromeDriver;
	private String openUrl = "";
	private String userAgent;
	private String platform;
	private String userAgentDataPlatform;
	private String acceptLanguage;
	private String[] languagesArray;

	public SeleniumDriverManager(boolean isHeadless, long waitDurationSeconds) {
		this(waitDurationSeconds, USER_AGENT_DEFAULT_VERSION);
		driverInit(isHeadless, "");
	}

	public SeleniumDriverManager(
			boolean isHeadless,
			long waitDurationSeconds,
			String chromeUserDataPath,
			String userAgentVersion
	) {
		this(waitDurationSeconds, userAgentVersion);
		driverInit(isHeadless, chromeUserDataPath);
	}

	private SeleniumDriverManager(long waitDurationSeconds, String userAgentVersion) {
		this.waitDurationSeconds = Duration.ofSeconds(waitDurationSeconds);
		this.userAgentVersion = userAgentVersion.isEmpty() ? USER_AGENT_DEFAULT_VERSION : userAgentVersion;
		createUserPlatformData();
		createUserLocaleData();
	}

	public WebElement findElement(String xpath) {
		return chromeDriver.findElement(By.xpath(xpath));
	}

	public String parse(String xpath) {
		WebElement element = findElement(xpath);
		return element.getText();
	}

	public Map<String, String> parse(HashSet<String> xpathSet) {
		Map<String, String> parseMap = new HashMap<>();
		for (String xpath : xpathSet) {
			parseMap.put(xpath, parse(xpath));
		}
		return parseMap;
	}

	public Map<String, String> parseFrom(String url, HashSet<String> xpathSet) {
		openPage(url);
		return parse(xpathSet);
	}


	public void click(boolean newTab, String... xpathArray) {
		for (String xpath : xpathArray) {
			click(newTab, xpath);
		}
	}

	public void click(String... xpathArray) {
		for (String xpath : xpathArray) {
			click(false, xpath);
		}
	}

	public void move(String xpath) {
		Actions actions = new Actions(chromeDriver);
		actions.moveToElement(findElement(xpath)).perform();
	}


	public void setText(String xpath, String text) {
		WebElement element = findElement(xpath);
		element.clear();
		element.sendKeys(text);
	}

	public void addText(String xpath, String text) {
		WebElement element = findElement(xpath);
		element.sendKeys(Keys.END);
		element.sendKeys(text);
	}


	public void openPage(String url) {
		openUrl = url;
		chromeDriver.get(url);
	}


	public int getTabsCount() {
		ArrayList<String> tabs = new ArrayList<>(chromeDriver.getWindowHandles());
		return tabs.size();
	}

	public void goToTab(int index) {
		chromeDriver.switchTo().window(new ArrayList<>(chromeDriver.getWindowHandles()).get(index));
	}

	public void updatePage() {
		chromeDriver.navigate().refresh();
	}

	public void goToLastTab() {
		goToTab(getTabsCount() - 1);
	}

	public void goToFirstTab() {
		goToTab(0);
	}

	public void closeTab() {
		chromeDriver.close();
	}


	public void closeDriver() {
		chromeDriver.quit();
	}

	public String getOpenUrl() {
		return openUrl;
	}


	private void createUserPlatformData() {
		String osName = System.getProperty("os.name").toLowerCase().trim();
		String platformDescription;
		if (osName.contains("win")) {
			platformDescription = "Windows NT 10.0; Win64; x64";
			platform = "Win32";
			userAgentDataPlatform = "Windows";
		} else if (osName.contains("mac")) {
			platformDescription = "Macintosh; Intel Mac OS X 10_15_7";
			platform = "MacIntel";
			userAgentDataPlatform = "macOS";
		} else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
			platformDescription = "X11; Linux x86_64";
			platform = "Linux x86_64";
			userAgentDataPlatform = "Linux";
		} else {
			throw new IllegalStateException("Unknown OS: " + osName);
		}

		userAgent = String.format(USER_AGENT_SCRIPT, platformDescription, userAgentVersion);
	}


	private void createUserLocaleData() {
		Locale locale = Locale.getDefault();
		String languageTag = locale.toLanguageTag();
		String language = locale.getLanguage();

		if (!languageTag.equalsIgnoreCase(language)) {
			acceptLanguage = languageTag + "," + language + ";q=0.9";
			languagesArray = new String[] { languageTag, language };
		} else {
			acceptLanguage = languageTag;
			languagesArray = new String[] { languageTag };
		}
	}


	private void driverInit(boolean isHeadless, String chromeUserDataPath) {

		ChromeOptions options = initOptions(isHeadless, chromeUserDataPath);
		chromeDriver = new ChromeDriver(options);

		String chromeSettingsScript = getSettingsScript();
		chromeDriver.executeScript(chromeSettingsScript);

		manageChromeDevTools();

		chromeDriver.manage().timeouts().implicitlyWait(waitDurationSeconds);
	}


	private ChromeOptions initOptions(boolean isHeadless, String chromeUserDataPath) {

		ChromeOptions options = new ChromeOptions();
		if (isHeadless) {
			options.addArguments("--headless");
		} else {
			options.addArguments("start-maximized");
		}

		options.addArguments("user-agent=" + userAgent);
		options.addArguments("--no-first-run");
		options.addArguments("--no-default-browser-check");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--disable-features=OptimizationGuideModelDownloading,OptimizationHintsFetching");
		options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
		options.setExperimentalOption("useAutomationExtension", false);
		options.addArguments("--disable-blink-features=AutomationControlled");
		options.addArguments("--disable-infobars");

		if (chromeUserDataPath == null || chromeUserDataPath.isEmpty()) {
			return options;
		}

		File userData = new File(chromeUserDataPath);
		if (!userData.exists() && !userData.mkdir()) {
			throw new RuntimeException("Can`t find and create path:" + chromeUserDataPath);
		}

		options.addArguments(String.format("--user-data-dir=%s", userData.getAbsolutePath()));
		options.addArguments("--profile-directory=Profile 1");

		return options;
	}


	private String getSettingsScript() {
		return "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});" +
			   "Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3]});" +
			   "Object.defineProperty(navigator, 'languages', {get: () => ['" + String.join("','", languagesArray) + "']});" +
			   "Object.defineProperty(navigator, 'platform', {get: () => '" + platform + "'});" +
			   "Object.defineProperty(navigator, 'userAgentData', {get: () => ({brands: [" +
			   "{brand: 'Chromium', version: '" + userAgentVersion + "'}, " +
			   "{brand: 'Google Chrome', version: '" + userAgentVersion + "'}, " +
			   "{brand: 'Not A(Brand', version: '24'}], " +
			   "mobile: false, " +
			   "platform: '" + userAgentDataPlatform + "'" +
			   "})});";
	}


	private void manageChromeDevTools() {
		try {
			DevTools devTools = chromeDriver.getDevTools();
			devTools.createSession();
			devTools.send(Network.setUserAgentOverride(
					userAgent,
					Optional.of(acceptLanguage),
					Optional.of(userAgentDataPlatform),
					Optional.empty()
			));
		} catch (IllegalStateException | WebDriverException e) {
			System.err.println("Initialization error DevTools: " + e.getMessage());
		}
	}


	private void click(boolean newTab, String xpath) {
		WebElement element = findElement(xpath);
		WebDriverWait wait = new WebDriverWait(chromeDriver, waitDurationSeconds);
		if (newTab) {
			int tabsCount = getTabsCount();
			Actions actions = new Actions(chromeDriver);
			actions.keyDown(Keys.CONTROL)
					.click(element)
					.keyUp(Keys.CONTROL)
					.build()
					.perform();
			wait.until(ExpectedConditions.numberOfWindowsToBe(tabsCount + 1));
		} else {
			element.click();
		}
	}

}
