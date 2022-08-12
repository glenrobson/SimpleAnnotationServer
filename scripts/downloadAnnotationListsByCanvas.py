#!/usr/bin/python

import sys
import urllib.request
import json
import os
import base64, uuid
import re
import logging

CACHEDIR="cache"
if not os.path.exists(CACHEDIR):
    os.makedirs(CACHEDIR)

def cacheFilename(url):
    z=base64.b64encode(url.encode("utf-8")).decode("utf-8").rstrip('=\n').replace('/', '_')
    return z + '.json'

def fetch(url, retry=0):
    cached = os.path.join(CACHEDIR, cacheFilename(url))
    if os.path.exists(cached):
        logging.debug('Getting %s from cache: %s',url, cached)
        with open(cached, encoding = 'utf-8') as fh:
            data = json.loads(fh.read())
    else:
        request = urllib.request.Request(url)
        if '@' in url:
            result= re.search(r"\/\/(.*)@", url)
            url = re.sub(r"\/\/*.*@", r'//', url)
            request = urllib.request.Request(url)
            if result:
                base64string = base64.b64encode(result.group(1))
                request.add_header("Authorization", "Basic %s" % base64string)

        try:
            fh =  urllib.request.urlopen(request)
            data = json.loads(fh.read())
            fh.close()

            with open(cached, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=4)

        except urllib.request.HTTPError as error:
            data = None
            # 404 means no annotations for this canvas
            logging.error("Getting %s failed due to %s: %s (Rery: %s)", url, error.code, error.reason, retry)
            if error.code == 500 and retry < 5:
                return fetch(url, retry+1)
        except urllib.error.URLError as error:
            data = None
            # 404 means no annotations for this canvas
            logging.error("Failed to get %s due to %s. Do you have the correct URL for SAS and is it running?", url, error)

    return data
if __name__ == "__main__":
    logging.basicConfig( encoding='utf-8', level=logging.ERROR)
    if len(sys.argv) < 4:
        print("Usage:\n\tdownloadAnnotationListsByCanvas.py [manifest] [sas_endpoint] [output_dir] [optional outputfilename proc]")
        print ("Arg no = %s" % len(sys.argv))
        sys.exit(0)

    print ("Downloading manifest: {}".format(sys.argv[1]))
    manifest = fetch(sys.argv[1])
    if not manifest:
        print ('Failed to load manifest')
        exit(-1)
    sasEndpoint = sys.argv[2]
    if sasEndpoint.endswith('/'):
        # remove last slash
        sasEndpoint = sasEndpoint[:-1]

    count=0
    for canvas in manifest["sequences"][0]["canvases"]:
        count += 1
        annoListData = fetch("%s/annotation/search?uri=%s" % (sasEndpoint, canvas["@id"]))
        if annoListData:
            print ("Downloaded annotations for canvas: {} ".format(canvas["@id"]))
            # add list to resource
            annoList = {
                "@type" : "sc:AnnotationList",
                "context": "http://iiif.io/api/presentation/2/context.json",
                "resources": annoListData
            }
            if len(sys.argv) > 4 and sys.argv[4] == 'nlw':
                filename = canvas["@id"].split('/')[-1]
            else:
               filename = "page%s.json" % count

            outputDirectory = sys.argv[3]
            outFilename = "%s/%s" % (outputDirectory,filename)
            if not os.path.exists(outputDirectory):
                os.makedirs(outputDirectory)

            with open(outFilename, 'w') as outfile:
                json.dump(annoList, outfile, indent=4)
            print ('Saved file: {}'.format(outFilename))    
        #else:
        #    print ('No annotations for canvas: {}'.format(canvas['@id']))
        #except:
        #    print (annoListData)
