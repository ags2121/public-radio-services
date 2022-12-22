import queue, time, urllib.request
from threading import Thread
import feedparser
import pprint
import os


def perform_web_requests(urls: list[str], num_workers: int):
    class Worker(Thread):
        def __init__(self, request_queue):
            Thread.__init__(self)
            self.queue = request_queue
            self.results = []

        def run(self):
            while True:
                url = self.queue.get()
                if url == "":
                    break
                feed = feedparser.parse(url)

                links = []
                try:
                    links = feed["entries"][0]["links"]
                except:
                    pprint.pprint("Error fetching " + url)

                audio_link = [link["href"] for link in links if link["type"] == "audio/mpeg"]
                self.results.append(audio_link)
                self.queue.task_done()

    # Create queue and add urls
    q = queue.Queue()
    for url in urls:
        q.put(url)

    # Workers keep working till they receive an empty string
    for _ in range(num_workers):
        q.put("")

    # Create workers and add to the queue
    workers = []
    for _ in range(num_workers):
        worker = Worker(q)
        worker.start()
        workers.append(worker)

    # Join workers to wait till they finished
    for worker in workers:
        worker.join()

    # Combine results from all workers
    r = []
    for worker in workers:
        r.extend(worker.results)

    return r    


urls = [
    # "https://www.rumblestripvermont.com/feed", # returning errors
    "http://feeds.revealradio.org/revealpodcast.xml",
    "http://newyorkpubliclibrary.libsyn.com/rss",
    "http://www.bbc.co.uk/programmes/b006qykl/episodes/downloads.rss",
    "http://radioopensource.org/feed/",
    "http://feeds.feedburner.com/homebravepodcast",
    "https://www.theguardian.com/news/series/the-audio-long-read/podcast.xml",
    "https://www.kcrw.com/news-culture/shows/unfictional/rss.xml",
    "https://www.kcrw.com/news-culture/shows/the-organist/rss.xml",
    "http://www.bbc.co.uk/programmes/b01mk3f8/episodes/downloads.rss",
    "http://www.bbc.co.uk/programmes/p02pc9qx/episodes/downloads.rss",
    "http://feeds.soundcloud.com/users/soundcloud:users:169774121/sounds.rss",
    "http://feeds.feedburner.com/MouthTimeWithReductress",
    "http://feeds.wnyc.org/snapjudgment-wnyc",
    "http://feeds.feedburner.com/pri/world-words",
    "http://feeds.soundcloud.com/users/soundcloud:users:211911700/sounds.rss",
    "http://www.bbc.co.uk/programmes/b006qnmr/episodes/downloads.rss",
    "https://www.residentadvisor.net/xml/podcast.xml",
    "https://wfmu.org/podcast/AO.xml",
    "https://wfmu.org/podcast/HG.xml" ,
    "https://theintercept.com/feed/?lang=en",
    "http://www.bbc.co.uk/programmes/b006x2tq/episodes/downloads.rss",
    "http://www.bbc.co.uk/programmes/b006x3hl/episodes/downloads.rss",
    "http://feeds.feedburner.com/birdnote/OYfP",
    "http://feeds.soundcloud.com/users/soundcloud:users:292981343/sounds.rss",
    "http://feeds.soundcloud.com/users/soundcloud:users:200783566/sounds.rss",
    "https://deleteyouraccount.libsyn.com/showrss",
    "https://citationsneeded.libsyn.com/rss",
    "https://feeds.soundcloud.com/users/soundcloud:users:54379684/sounds.rss",
    "http://artandlaborpodcast.com/feed/podcast",
    "http://feeds.soundcloud.com/users/soundcloud:users:300222802/sounds.rss",
    "http://feeds.feedburner.com/streetfightradio?format=xml",
]

num_workers = os.environ.get("NUM_WORKERS")
if not num_workers:
    raise Exception("Ensure you pass NUM_WORKERS as an env var")
    
results = perform_web_requests(urls=urls, num_workers=8)
pprint.pprint(results)
