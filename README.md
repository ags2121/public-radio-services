# public radio services

```
python3 -m venv env
source env/bin/activate
pip install -i https://pypi.python.org/simple -r requirements.txt

NUM_WORKERS=8 python fetch_urls.py

curl "http://localhost:9000/2015-03-31/functions/function/invocations" -d '{}'

# open chrome browser
open -na Google\ Chrome --args --user-data-dir=/tmp/temporary-chrome-profile-dir --disable-web-security

```