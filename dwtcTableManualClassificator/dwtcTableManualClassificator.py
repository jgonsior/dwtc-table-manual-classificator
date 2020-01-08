import glob
import gzip
import os
from collections import defaultdict
from io import BytesIO
from pprint import pprint
from random import random
from urllib.parse import urlsplit

import click
import requests
import ujson
from flask import Flask, render_template
from flask_bootstrap import Bootstrap
from flask_sqlalchemy import SQLAlchemy

app = Flask(__name__)  # create the application instance :)
Bootstrap(app)
app.config.from_object(__name__)  # load config from this file

app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///data.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SQLALCHEMY_ECHO'] = False
db = SQLAlchemy(app)

app.config.from_envvar('TABLE_BROWSER_SETTINGS', silent=True)


class Table(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    pageTitle = db.Column(db.Text)
    url = db.Column(db.Text)
    title = db.Column(db.Text)
    originalTableType = db.Column(db.Text)
    cells = db.Column(db.Text)
    newTableType = db.Column(db.Text)
    label = db.Column(db.Text)
    tempTableType = db.Column(db.Text)
    recordEndOffset = db.Column(db.Text)
    recordOffset = db.Column(db.Text)
    s3Link = db.Column(db.Text)
    tableNum = db.Column(db.Text)

    def __init__(self, pageTitle, url, title, originalTableType, cells):
        self.pageTitle = pageTitle
        self.url = url
        self.title = title
        self.originalTableType = originalTableType
        self.cells = cells

    def __repr__(self):
        return '<Table %r>' % self.id


@app.cli.command('initDb')
@click.argument('sourcedirectory', nargs=1)
def initdbCommand(sourcedirectory):
    """Initializes the database."""
    db.create_all()

    counter = defaultdict(int)
    domainLimit = defaultdict(int)

    # first unzip file
    for filename in glob.glob(os.path.join(sourcedirectory, '*.json.gz')):
        print("Start processing " + filename)
        with gzip.open(filename, "rb") as file:
            chances_selected = 20 / sum(1 for line in file)

        with gzip.open(filename, "rb") as file:
            # then randomly select 20 tables from each file -> 500*20= 10 000
            # saving only a maximum of 100 tables of each domain
            for line in file:
                if random() < chances_selected:
                    # and finally save each entry into a new database
                    rawData = ujson.loads(line)
                    counter[rawData['tableType']] += 1
                    domain = "{0.scheme}://{0.netloc}/" \
                        .format(urlsplit(rawData['url']))
                    domainLimit[domain] += 1
                    if (counter[rawData['tableType']] < 2000
                            and domainLimit[domain] < 100):
                        table = Table(rawData['pageTitle'], rawData['url'],
                                      rawData['title'], rawData['tableType'],
                                      ujson.dumps(rawData['relation']))
                        db.session.add(table)
            db.session.commit()
    pprint(counter)
    pprint(domainLimit)

    print('Initialized the database')


@app.cli.command('generateMoreTrainingData')
@click.argument('sourcedirectory', nargs=1)
@click.argument('tabletype', nargs=1)
def generateMoreTrainingDataCommand(sourcedirectory, tabletype):
    """Initializes the database."""
    db.create_all()

    counter = defaultdict(int)
    domainLimit = defaultdict(int)

    # first unzip file
    for filename in glob.glob(os.path.join(sourcedirectory, '*.json.gz')):
        print("Start processing " + filename)
        with gzip.open(filename, "rb") as file:
            chances_selected = 200 / sum(1 for line in file)

        i = 0

        with gzip.open(filename, "rb") as file:
            for line in file:
                if random() < chances_selected:
                    rawData = ujson.loads(line)
                    counter[rawData['tableType']] += 1
                    domain = "{0.scheme}://{0.netloc}/" \
                        .format(urlsplit(rawData['url']))
                    domainLimit[domain] += 1
                    if (counter[rawData['tableType']] < 2000
                            and domainLimit[domain] < 100
                            and rawData['tableType'] == tabletype):
                        table = Table(rawData['pageTitle'], rawData['url'],
                                      rawData['title'], rawData['tableType'],
                                      ujson.dumps(rawData['relation']))
                        i += 1
                        db.session.add(table)
            db.session.commit()
    pprint(counter)
    pprint(domainLimit)

    print('Added  ' + str(i) + ' more tables of type ' + tabletype)


@app.cli.command('getS3Links')
@click.argument('sourcedirectory', nargs=1)
def getS3Links(sourcedirectory):
    """Initializes the database."""
    db.create_all()

    # load all entries out of the database
    tablesList = Table.query.all()

    tablesFinished = list()

    tables = {}
    tables['MATRIX'] = set()
    tables['RELATION'] = set()
    tables['ENTITY'] = set()
    tables['OTHER'] = set()

    for table in tablesList:
        tables[table.originalTableType].add(table)

    # first unzip file
    for filename in glob.glob(os.path.join(sourcedirectory, '*.json.gz')):
        print("Start searching in " + filename)
        with gzip.open(filename, "rb") as file:
            for line in file:
                rawData = ujson.loads(line)
                count = 0
                for table in tables[rawData['tableType']]:
                    if table.url == rawData['url']:
                        if table.cells == ujson.dumps(rawData['relation']):
                            count += 1
                            if (count > 1):
                                print("#" * 100)
                                print("duplicate found!!!")
                                pprint(rawData)
                            table.recordEndOffset = rawData['recordEndOffset']
                            table.recordOffset = rawData['recordOffset']
                            table.s3Link = rawData['s3Link']
                            table.tableNum = rawData['tableNum']
                            tablesFinished.append(table)
                            db.session.add(table)
        pprint(tablesFinished)
        db.session.commit()


@app.cli.command('getOriginalHtmlFromS3')
def getOriginalHtmlFromS3():
    """Initializes the database."""

    db.create_all()

    # load all entries out of the database
    tables = Table.query.all()

    # for key in publicDatasets.list():
    #    print(key.name.encode('utf-8'))

    for table in tables:
        print("Downloading https://commoncrawl.s3.amazonaws.com/" +
              table.s3Link[13:])
        response = requests.get(
            "https://commoncrawl.s3.amazonaws.com/" + table.s3Link[13:],
            headers={
                'Range':
                'bytes={}-{}'.format(table.recordOffset, table.recordEndOffset)
            })

        raw_data = BytesIO(response.content)
        # stringio = StringIO(raw_data.read().decode('latin-1'))
        with gzip.GzipFile(fileobj=raw_data) as f:
            print("whut")
            pprint(f.read())

        print("Done…")
        '''with gzip.open("warc.gz", "r") as file:
            for line in file:
                print(line)'''
        return

        # common-crawl/crawl-data/CC-MAIN-2014-23/segments/1405997894799.55/warc/CC-MAIN-20140722025814-00066-ip-10-33-131-23.ec2.internal.warc.gz
        # key = Key(publicDatasets, "crawl-data/CC-MAIN-2014-23/segments/1404776400583.60/warc/CC-MAIN-20140707234000-00023-ip-10-180-212-248.ec2.internal.warc.gz")
        # "s3://commoncrawl/crawl-data/CC-MAIN-2014-23/segments/1404776400583.60/warc/CC-MAIN-20140707234000-00001-ip-10-180-212-248.ec2.internal.warc.gz" #+ table.s3Link
        # print(key)
        # print(key.read(100))


@app.route('/')
@app.route('/<int:page>')
def showTables(page=1):
    entries = db.session.query(Table).paginate(page, 100)
    #  entries = db.session.query(Table).filter(
    #  Table.newTableType != Table.label).paginate(page, 100)
    return render_template('show_tables.jinja2', entries=entries)


@app.route('/show/<int:pageId>')
def showTable(pageId):
    entries = db.session.query(Table).paginate(pageId, 1)
    #  entries = db.session.query(Table).filter(
    #  Table.newTableType != Table.label).paginate(pageId, 1)
    #entries = db.session.query(Table).filter(
    #    Table.label == "MATRIX").paginate(pageId, 1)
    meta = entries.items[0]
    table = ujson.loads(meta.cells)

    # should be moved into table class
    meta.domain = "{0.scheme}://{0.netloc}/".format(urlsplit(meta.url))
    next = prev = pageId

    if entries.has_prev:
        prev = entries.prev_num
    if entries.has_next:
        next = entries.next_num
    return render_template('show_table.jinja2',
                           meta=meta,
                           table=table,
                           prev=prev,
                           next=next)


@app.route('/changeClass/<int:tableId>/<string:newTableType>')
def changeClass(tableId, newTableType):
    table = Table.query.get(tableId)
    table.tempTableType = newTableType
    db.session.commit()
    return ujson.dumps({'success': True}), 200, \
           {'ContentType': 'application/json'}
