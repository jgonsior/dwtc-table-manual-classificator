# dwtc-table-browser
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0)

A tool for manual classification of dwtc tables. The result can then be used as a training data set.

## Instructions
1. Download a DWTC dataset from https://wwwdb.inf.tu-dresden.de/research-projects/dresden-web-table-corpus/ into the `data` folder
2. Let pip install all needed requirements via `pip install -r requirements.txt`
3. `export FLASK_APP=tableBrowser`
4. `flask initDb data/dwtc-000.json.gz` to initially put from the downloaded dwtc tables 5000 of each type into the SQLite database
5. Run the program with `./start.sh`
6. Go to http://127.0.0.1:5000/
7. Have fun classifiying :)

## License
Unless explicitly noted otherwise, the content of this package is released under the [GNU Affero General Public License version 3 (AGPLv3)](http://www.gnu.org/licenses/agpl.html)

[Why the GNU Affero GPL](http://www.gnu.org/licenses/why-affero-gpl.html) (short answer: why not?)

Copyright © 2017 [Julius Gonsior](https://gaenseri.ch/)
