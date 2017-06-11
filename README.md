i# dwtc-table-manual-classificator
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0)

A tool for manual classification of dwtc tables. The result can then be used as a training data set.

## Instructions
1. Download as much DWTC datasets from https://wwwdb.inf.tu-dresden.de/research-projects/dresden-web-table-corpus/ as you want
2. Let pip install all needed requirements via `pip install -r requirements.txt`
3. `export FLASK_APP=dwtc-table-manual-classificator`
4. `pip install --editable .`
5. `flask initDb pathToDwtcFiles/` to extract randomly 20 tables from each file, but saving a maximum of 100 tables per domain in the SQLite database
6. Run the program with `./start.sh`
7. Go to http://127.0.0.1:5000/
8. Have fun classifiying :)

## License
Unless explicitly noted otherwise, the content of this package is released under the [GNU Affero General Public License version 3 (AGPLv3)](http://www.gnu.org/licenses/agpl.html)

[Why the GNU Affero GPL](http://www.gnu.org/licenses/why-affero-gpl.html) (short answer: why not?)

Copyright © 2017 [Julius Gonsior](https://gaenseri.ch/)
