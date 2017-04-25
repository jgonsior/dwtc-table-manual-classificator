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

app = Flask(__name__)  # create the application instance :)
Bootstrap(app)
app.config.from_object(__name__)  # load config from this file

app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///data.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
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

    def __init__(self, pageTitle, url, title, originalTableType, cells):
        self.pageTitle = pageTitle
        self.url = url
        self.title = title
        self.originalTableType = originalTableType
        self.cells = cells

    def __repr__(self):
        return '<Table %r>' % self.id


@app.cli.command('initDb')
@click.argument('sourceFile')
def initdbCommand(sourcefile):
    """Initializes the database."""
    db.create_all()

    # first unzip file
    with gzip.open(sourcefile, "rb") as file:
        # then iterate over it line by line
        # saving only a maximum of 5000 tables of each kind
        tableLimits = defaultdict(int)
        for line in file:
            # and finally save each entry into a new database
            rawData = ujson.loads(line)
            if tableLimits[rawData['tableType']] < 5000:
                table = Table(rawData['pageTitle'], rawData['url'],
                              rawData['title'], rawData['tableType'],
                              ujson.dumps(rawData['relation']))
                db.session.add(table)
                tableLimits[rawData['tableType']] += 1
        # could be optimized, but only if needed to :)
        db.session.commit()

    print('Initialized the database')


@app.route('/')
@app.route('/<int:page>')
def showTables(page=1):
    entries = db.session.query(Table).paginate(page, 100)
    return render_template('show_tables.jinja2', entries=entries)


@app.route('/show/<int:tableId>')
def showTable(tableId):
    meta = Table.query.get(tableId)
    table = ujson.loads(meta.cells)
    return render_template('show_table.jinja2', meta=meta, table=table)


@app.route('/changeClass/<int:tableId>/<string:newTableType>')
def changeClass(tableId, newTableType):
    table = Table.query.get(tableId)
    table.newTableType = newTableType
    db.session.commit()
    return ujson.dumps({'success': True}), 200, {'ContentType': 'application/json'}
