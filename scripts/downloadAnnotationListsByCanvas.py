#!/usr/bin/python

import sys
import urllib2
import json
import os
import base64, uuid
import re

CACHEDIR="cache"
if not os.path.exists(CACHEDIR):
    os.makedirs(CACHEDIR)

def cacheFilename(url):
    z=base64.b64encode(url).rstrip('=\n').replace('/', '_')
    return z + '.json'

def fetch(url, retry=0):
    cached = os.path.join(CACHEDIR, cacheFilename(url))
    if os.path.exists(cached):
        fh = file(cached)
        data = fh.read()
        fh.close()
    else:
        request = urllib2.Request(url)
        if '@' in url:
            result= re.search(r"\/\/(.*)@", url)
            url = re.sub(r"\/\/*.*@", r'//', url)
            request = urllib2.Request(url)
            if result:
                base64string = base64.b64encode(result.group(1))
                request.add_header("Authorization", "Basic %s" % base64string)

        try: 
            fh = urllib2.urlopen(request)
            data = fh.read()
            fh.close()
            fh = file(cached, 'w')
            fh.write(data)
            fh.close()
        except urllib2.HTTPError as error:
            print("Getting " + url + " failed due to " + str(error.code) + " " + error.reason + " retry " + str(retry))
            if error.code == 500 and retry < 5:
                return fetch(url, retry+1)

    return data
if __name__ == "__main__":    

    if len(sys.argv) < 4:
        print("Usage:\n\tdownloadAnnotationListsByCanvas.py [manifest] [sas_endpoint] [output_dir] [optional outputfilename proc]")
        print ("Arg no = %s" % len(sys.argv))
        sys.exit(0)

    print ("Downloading manifest")
    manifest = json.loads(fetch(sys.argv[1]))

    count=0
    for canvas in manifest["sequences"][0]["canvases"]:
        count += 1
        print ("Downloading %s " % canvas["@id"])
        annoListData = fetch("%s/annotation/search?uri=%s" % (sys.argv[2], canvas["@id"]))
        #try: 
        annoList = json.loads(annoListData)
        if len(sys.argv) > 4 and sys.argv[4] == 'nlw':
            filename = canvas["@id"].split('/')[-1]
        else:
           filename = "page%s.json" % count
        with open("%s/%s" % (sys.argv[3],filename), 'wb') as outfile:
            json.dump(annoList, outfile, sort_keys=False,indent=4, separators=(',', ': '))
        outfile.close()    
        #except:
        #    print (annoListData)


