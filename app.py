import get_feeds_wrapper
import json

def handler(event, context):
    feeds = get_feeds_wrapper.perform_web_requests()
    return {
        "statusCode": 200,
        "headers": {"Content-Type": "application/json"},
        "body": feeds,
    }
