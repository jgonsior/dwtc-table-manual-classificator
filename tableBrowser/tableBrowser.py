import os
import sqlite3
import ujson
from flask import Flask, request, session, g, redirect, url_for, abort, \
    render_template, flash
from pprint import pprint

app = Flask(__name__)  # create the application instance :)
app.config.from_object(__name__)  # load config from this file

# Load default config and override config from an environment variable
app.config.update(dict(
    DATABASE=os.path.join(app.root_path, 'data.db')
    #    SECRET_KEY='development key',
    #    USERNAME='admin',
    #    PASSWORD='default'
))
app.config.from_envvar('TABLE_BROWSER_SETTINGS', silent=True)


def connect_db():
    """Connects to the specific database."""
    rv = sqlite3.connect(app.config['DATABASE'])
    rv.row_factory = sqlite3.Row
    return rv


def get_db():
    """Opens a new database connection if there is none yet for the
    current application context.
    """
    if not hasattr(g, 'sqlite_db'):
        g.sqlite_db = connect_db()
    return g.sqlite_db


@app.teardown_appcontext
def close_db(error):
    """Closes the database again at the end of the request."""
    if hasattr(g, 'sqlite_db'):
        g.sqlite_db.close()


@app.route('/')
def showTables():
    db = get_db()
    c = db.execute('SELECT * FROM tables LIMIT 100')
    entries = c.fetchall()
    return render_template('show_tables.html', entries=entries)


@app.route('/show/<int:tableId>')
def showTable(tableId):
    c = get_db().execute('SELECT * FROM tables WHERE id=(?)', (tableId,))
    meta = c.fetchone()
    table=ujson.loads(meta['cells'])
    return render_template('show_table.html', meta=meta, table=table)
