import queue, time, urllib.request
from threading import Thread
import feedparser
import pprint
import os


def _perform_web_requests(feeds, num_workers):
    class Worker(Thread):
        def __init__(self, request_queue):
            Thread.__init__(self)
            self.queue = request_queue
            self.results = []

        def run(self):
            while True:
                feed = self.queue.get()
                if feed == "":
                    break

                url = feed["url"]
                feed_result = feedparser.parse(feed["url"])

                title = feed_result.feed.title
                description = feed_result.feed.description

                audio_link = None
                episode_title = None
                pub_date = None
                try:
                    latest_entry = feed_result["entries"][0]
                    links = latest_entry["links"]
                    audio_links = [
                        link["href"] for link in links if link["type"] == "audio/mpeg"
                    ]
                    audio_link = audio_links[0]
                    pub_date = latest_entry.published
                    episode_title = latest_entry["title"]
                except:
                    pprint.pprint("Error fetching links for " + url)

                feed["url"] = audio_link
                feed["pubDate"] = pub_date
                feed["showTitle"] = title
                feed["showDescription"] = description
                feed["episodeTitle"] = episode_title

                self.results.append(feed)
                self.queue.task_done()

    # Create queue and add feeds
    q = queue.Queue()
    for feed in feeds:
        q.put(feed)

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


def perform_web_requests(num_workers: int):
    feeds = [
        {
            "name": "reveal",
            "type": "podcast",
            "url": "http://feeds.revealradio.org/revealpodcast.xml",
        },
        {
            "name": "in-our-time",
            "type": "podcast",
            "url": "http://www.bbc.co.uk/programmes/b006qykl/episodes/downloads.rss",
        },
        {
            "name": "open-source",
            "type": "podcast",
            "url": "http://radioopensource.org/feed/",
        },
        {
            "name": "homebrave",
            "type": "podcast",
            "url": "http://feeds.feedburner.com/homebravepodcast",
        },
        {
            "name": "guardian",
            "type": "podcast",
            "url": "https://www.theguardian.com/news/series/the-audio-long-read/podcast.xml",
        },
        {
            "name": "unfictional",
            "type": "podcast",
            "url": "https://www.kcrw.com/news-culture/shows/unfictional/rss.xml",
        },
        {
            "name": "organist",
            "type": "podcast",
            "url": "https://www.kcrw.com/news-culture/shows/the-organist/rss.xml",
        },
        {
            "name": "shortcuts",
            "type": "podcast",
            "url": "http://www.bbc.co.uk/programmes/b01mk3f8/episodes/downloads.rss",
        },
        {
            "name": "seriously",
            "type": "podcast",
            "url": "http://www.bbc.co.uk/programmes/p02pc9qx/episodes/downloads.rss",
        },
        {
            "name": "mouth-time",
            "type": "podcast",
            "url": "http://feeds.feedburner.com/MouthTimeWithReductress",
        },
        {
            "name": "snapjudgement",
            "type": "podcast",
            "url": "http://feeds.wnyc.org/snapjudgment-wnyc",
        },
        {
            "name": "worldinwords",
            "type": "podcast",
            "url": "http://feeds.feedburner.com/pri/world-words",
        },
        {
            "name": "chapos-traphouse",
            "type": "podcast",
            "url": "http://feeds.soundcloud.com/users/soundcloud:users:211911700/sounds.rss",
        },
        {
            "name": "desert-island-discs",
            "type": "podcast",
            "url": "http://www.bbc.co.uk/programmes/b006qnmr/episodes/downloads.rss",
        },
        {
            "name": "resident-advisor",
            "type": "music",
            "url": "https://www.residentadvisor.net/xml/podcast.xml",
        },
        {
            "name": "eternal-now",
            "type": "music",
            "url": "https://wfmu.org/podcast/AO.xml",
        },
        {
            "name": "honky-tonk",
            "type": "music",
            "url": "https://wfmu.org/podcast/HG.xml",
        },
        {
            "name": "between-the-ears",
            "type": "podcast",
            "url": "http://www.bbc.co.uk/programmes/b006x2tq/episodes/downloads.rss",
        },
        {
            "name": "the-essay",
            "type": "podcast",
            "url": "http://www.bbc.co.uk/programmes/b006x3hl/episodes/downloads.rss",
        },
        {
            "name": "bird-note",
            "type": "podcast",
            "url": "http://feeds.feedburner.com/birdnote/OYfP",
        },
        {
            "name": "upstream",
            "type": "podcast",
            "url": "http://feeds.soundcloud.com/users/soundcloud:users:200783566/sounds.rss",
        },
        {
            "name": "delete-your-acct",
            "type": "podcast",
            "url": "https://deleteyouraccount.libsyn.com/showrss",
        },
        {
            "name": "citations",
            "type": "podcast",
            "url": "https://citationsneeded.libsyn.com/rss",
        },
        {
            "name": "kt-halps",
            "type": "podcast",
            "url": "https://feeds.soundcloud.com/users/soundcloud:users:54379684/sounds.rss",
        },
        {
            "name": "art-and-labor",
            "type": "podcast",
            "url": "http://artandlaborpodcast.com/feed/podcast",
        },
        {
            "name": "trillbilly",
            "type": "podcast",
            "url": "http://feeds.soundcloud.com/users/soundcloud:users:300222802/sounds.rss",
        },
        {
            "name": "street-fight",
            "type": "podcast",
            "url": "http://feeds.feedburner.com/streetfightradio?format=xml",
        },
    ]
    return _perform_web_requests(feeds=feeds, num_workers=num_workers)
