import sys
import ujson
import gzip
import time
import sqlite3

from pprint import pprint

HELP_TEXT = '\033[1mexample.py\033[0m dump'

DB_OUTPUT = 'data.db'
connection = sqlite3.connect(DB_OUTPUT)
c = connection.cursor()
c.execute('''CREATE TABLE tables (
                    id INTEGER PRIMARY KEY AUTOINCREMENT , 
                    pageTitle TEXT,
                    url TEXT,
                    title TEXT, 
                    tableType TEXT,
                    cells TEXT
                )''')

# get arguments
argc = len(sys.argv)
argv = sys.argv

if argc > 1:
    # first unzip file
    with gzip.open(argv[1], "rb") as file:
        #then iterate over it line by line
        for line in file:
            #and finally save each entry into a new database
            rawData = ujson.loads(line)
            data = (rawData['pageTitle'], rawData['url'], rawData['title'],
                    rawData['tableType'], ujson.dumps(rawData['relation']))
            c.execute("INSERT INTO tables VALUES(Null,?,?,?,?,?)", data)
        connection.commit()
        connection.close()
else:
    print(HELP_TEXT)
