import get_feeds_wrapper
import json
import pprint

def convert_to_map(_list):
    return {el["name"]: el for el in _list}

def handler(event, context):
    feed_type = event.get("feed_type")
    if not feed_type:
        raise Exception("Please pass in a value for `feed_type` in your request")

    feeds = get_feeds_wrapper.perform_web_requests(feed_type=feed_type)
    
    return dict(podcasts=convert_to_map(feeds))
    # return {
    #     "statusCode": 200,
    #     "headers": {
    #         'Access-Control-Allow-Headers': '*',
    #         'Access-Control-Allow-Origin': '*',
    #         'Access-Control-Allow-Methods': '*',
    #         "Access-Control-Allow-Credentials": "true",
    #         "Content-Type": "application/json"
    #     },
    #     "body": convert_to_map(feeds),
    # }
