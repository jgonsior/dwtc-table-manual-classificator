import gzip

import requests

try:
    from cStringIO import StringIO
except:
    from StringIO import StringIO

offset = 375128872
offset_end = 375154939
prefix = ' https://commoncrawl.s3.amazonaws.com/'
resp = requests.get(
    prefix + "crawl-data/CC-MAIN-2014-23/segments/1404776432874.14/warc/CC-MAIN-20140707234032-00010-ip-10-180-212-248.ec2.internal.warc.gz",
    headers={'Range': 'bytes={}-{}'.format(offset, offset_end)})

raw_data = StringIO(resp.content)
f = gzip.GzipFile(fileobj=raw_data)

# What we have now is just the WARC response, formatted:
data = f.read()
warc, header, response = data.strip().split('\r\n\r\n', 2)
#
print
'WARC headers'
print
'---'
print
warc[:100]
print
'---'
print
'HTTP headers'
print
'---'
print
header[:100]
print
'---'
print
'HTTP response'
print
'---'
print
response[:100]
