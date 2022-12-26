import get_feeds_wrapper
import json

def convert_to_map(_list):
    return {el["name"]: el for el in _list}

def handler(event, context):
    feeds = get_feeds_wrapper.perform_web_requests()
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
