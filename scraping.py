from selenium import webdriver
from selenium.webdriver.firefox.service import Service
from selenium.webdriver.firefox.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time
import json

# twitter credentials (ya i hardcoded them sorry but i was only going to run this a couple times locally)
TWITTER_USERNAME = "" 
TWITTER_PASSWORD = ""  

if not TWITTER_USERNAME or not TWITTER_PASSWORD:
    raise ValueError("twitter credentials not set.")

firefox_options = Options()
firefox_options.add_argument("--headless")  
firefox_options.add_argument("--disable-gpu")
firefox_options.add_argument("--no-sandbox")

# WebDriver for Firefox
service = Service()  # if GeckoDriver is installed and in PATH, no need to specify the path
driver = webdriver.Firefox(service=service, options=firefox_options)

def login_to_twitter():
    print("Logging in to Twitter...")
    driver.get("https://x.com/login")
    wait = WebDriverWait(driver, 30)
    
    username_input = wait.until(EC.presence_of_element_located((By.NAME, "text")))
    username_input.send_keys(TWITTER_USERNAME)
    username_input.send_keys(Keys.RETURN)
    print("Entered username.")
    time.sleep(2)

    try:
        next_button = wait.until(EC.element_to_be_clickable((By.XPATH, "//span[text()='Next']")))
        next_button.click()
        print("Clicked 'Next' button after username")
        time.sleep(2)
    except:
        print("No next button, moving to password input")
    
    password_input = wait.until(EC.presence_of_element_located((By.NAME, "password")))
    password_input.send_keys(TWITTER_PASSWORD)
    password_input.send_keys(Keys.RETURN)
    print("Entered password.")

    # wait for successful login by checking for the search input
    try:
        wait = WebDriverWait(driver, 30)
        search_input = wait.until(EC.presence_of_element_located((By.XPATH, "//input[@aria-label='Search query']")))
        print("Logged in to Twitter successfully!")
    except Exception as e:
        print("error after entering password: could not find search input.")
        print(f"Exception: {e}")
        print(f"Current page title: {driver.title}")
        print(f"Current URL: {driver.current_url}")

def scrape_tweets(search_query, max_tweets=100):  
    print(f"Navigating to Explore page for query: {search_query}...")
    driver.get("https://x.com/explore")
    
    # wait for the search input element to be present
    wait = WebDriverWait(driver, 30)
    search_input = wait.until(EC.presence_of_element_located((By.XPATH, "//input[@aria-label='Search query']")))
    print("Search input found, entering search query.")
    
    search_input.send_keys(search_query)
    search_input.send_keys(Keys.RETURN)
    print("Search query entered, waiting for results...")

    # wait for tweet elements to load dynamically!
    tweets = []
    tweet_ids = set()
    tweet_container_xpath = "//article"  

    print("Starting to scrape tweets...")
    while len(tweets) < max_tweets:
        try:
            wait.until(EC.presence_of_element_located((By.XPATH, tweet_container_xpath)))
            tweet_elements = driver.find_elements(By.XPATH, tweet_container_xpath)
            print(f"Found {len(tweet_elements)} tweet elements on the page.")

            for tweet in tweet_elements:
                try:
                    # js execution to get dynamically loaded tweet text
                    content = driver.execute_script("return arguments[0].innerText;", tweet)
                    if content and content not in tweet_ids:
                        print(f"Scraped tweet: {content.strip()}")  
                        tweets.append(content.strip())
                        tweet_ids.add(content.strip())
                        if len(tweets) >= max_tweets:
                            print(f"Reached the cap of {max_tweets} tweets.")
                            return tweets
                except Exception as e:
                    print(f"Error while retrieving tweet text: {e}")

            # scroll down to load more tweets
            driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
            time.sleep(5)  # Increased wait time after scrolling
            print(f"Collected {len(tweets)} tweets so far. Scrolling for more...")
        
        except Exception as e:
            print(f"Error while waiting for tweets to load: {e}")
            break

    print(f"Scraping complete. Collected {len(tweets)} tweets.")
    return tweets

login_to_twitter()

shows = ['The Mandalorian', 'The Acolyte', 'Andor']
tweet_data = {}

for show in shows:
    print(f"Scraping tweets for {show}...")
    tweets = scrape_tweets(f"{show} lang:en", max_tweets=20) 
    tweet_data[show] = tweets

# save to JSON
with open('tweets.json', 'w', encoding='utf-8') as f:
    json.dump(tweet_data, f, ensure_ascii=False, indent=4)

driver.quit()
print("Done. Saved all tweets to tweets.json.")