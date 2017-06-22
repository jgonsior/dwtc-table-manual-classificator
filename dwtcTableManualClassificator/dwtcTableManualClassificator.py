import os
import sqlite3
import ujson
import click
import gzip
from flask import Flask, request, session, g, redirect, url_for, abort, \
    render_template, flash
from flask_bootstrap import Bootstrap
from flask_sqlalchemy import SQLAlchemy
from pprint import pprint
from collections import defaultdict
import glob
from random import random
from urllib.parse import urlsplit

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
                    domain = "{0.scheme}://{0.netloc}/"\
                        .format(urlsplit(rawData['url']))
                    domainLimit[domain] += 1
                    if(counter[rawData['tableType']] < 2000
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
                    domain = "{0.scheme}://{0.netloc}/"\
                        .format(urlsplit(rawData['url']))
                    domainLimit[domain] += 1
                    if(counter[rawData['tableType']] < 2000
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
                            if(count > 1):
                                print("#"*100)
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
        db.session.commit()
        db.session.commit()
        db.session.commit()
        db.session.commit()

@app.route('/')
@app.route('/<int:page>')
def showTables(page=1):
    # entries = db.session.query(Table).paginate(page, 100)
    entries = db.session.query(Table).filter(Table.newTableType != Table.label, Table.newTableType == "RELATION_V").paginate(page, 100)
    return render_template('show_tables.jinja2', entries=entries)


@app.route('/show/<int:pageId>')
def showTable(pageId):
    #entries = db.session.query(Table).paginate(pageId, 1)
    entries = db.session.query(Table).filter(Table.newTableType != Table.label, Table.newTableType == "RELATION_V").paginate(pageId, 1)
    meta = entries.items[0]
    table = ujson.loads(meta.cells)

    # should be moved into table class
    meta.domain = "{0.scheme}://{0.netloc}/".format(urlsplit(meta.url))
    next = prev = pageId

    if entries.has_prev:
        prev = entries.prev_num
    if entries.has_next:
        next = entries.next_num
    return render_template('show_table.jinja2', meta=meta, table=table,
                            prev=prev, next=next)

@app.route('/changeClass/<int:tableId>/<string:newTableType>')
def changeClass(tableId, newTableType):
    table = Table.query.get(tableId)
    table.tempTableType = newTableType
    db.session.commit()
    return ujson.dumps({'success': True}), 200, \
        {'ContentType': 'application/json'}
