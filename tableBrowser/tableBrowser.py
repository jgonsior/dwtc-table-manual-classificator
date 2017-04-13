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
    tableType = db.Column(db.Text)
    cells = db.Column(db.Text)

    def __init__(self, pageTitle, url, title, tableType, cells):
        self.pageTitle = pageTitle
        self.url = url
        self.title = title
        self.tableType = tableType
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
        for line in file:
            # and finally save each entry into a new database
            rawData = ujson.loads(line)
            table = Table(rawData['pageTitle'], rawData['url'],
                          rawData['title'], rawData['tableType'],
                          ujson.dumps(rawData['relation']))
            db.session.add(table)
        #could be optimized, but only if needed to :)
        db.session.commit()

    print('Initialized the database')


@app.route('/')
def showTables():
    entries = db.session.query(Table).paginate(1, 100)
    return render_template('show_tables.html', entries=entries)


@app.route('/show/<int:tableId>')
def showTable(tableId):
    c = get_db().execute('SELECT * FROM tables WHERE id=(?)', (tableId,))
    meta = c.fetchone()
    table = ujson.loads(meta['cells'])
    return render_template('show_table.html', meta=meta, table=table)
