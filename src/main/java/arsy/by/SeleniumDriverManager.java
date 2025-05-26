package arsy.by;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;

public class SeleniumDriverManager {

	private ChromeDriver chromeDriver;
	private final Duration waitDurationSeconds;
	private String openUrl = "";

	public SeleniumDriverManager(boolean headlessOn, long waitDurationSeconds) {
		this.waitDurationSeconds = Duration.ofSeconds(waitDurationSeconds);
		driverInit(headlessOn);
	}


	public Map<String, String> parse(HashSet<String> xpathSet) {
		Map<String, String> parseMap = new HashMap<>();
		for (String xpath : xpathSet) {
			WebElement element = chromeDriver.findElement(By.xpath(xpath));
			parseMap.put(xpath, element.getText());
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


	public void setText(String xpath, String text) {
		WebElement element = chromeDriver.findElement(By.xpath(xpath));
		element.clear();
		element.sendKeys(text);
	}

	public void addText(String xpath, String text) {
		WebElement element = chromeDriver.findElement(By.xpath(xpath));
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


	private void driverInit(boolean headlessOn) {

		ChromeOptions options = new ChromeOptions();
		if (headlessOn) {
			options.addArguments("--headless");
			options.addArguments("--no-sandbox");
		} else {
			options.addArguments("start-maximized");
		}

		chromeDriver = new ChromeDriver(options);

		chromeDriver.manage().timeouts().implicitlyWait(waitDurationSeconds);

	}


	private void click(boolean newTab, String xpath) {
		WebElement element = chromeDriver.findElement(By.xpath(xpath));
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
