import get_feeds
import os

def perform_web_requests():
    num_workers = os.environ.get("NUM_WORKERS")
    if not num_workers:
        raise Exception("Ensure you pass NUM_WORKERS as an env var")

    return get_feeds.perform_web_requests(num_workers=int(num_workers))
    