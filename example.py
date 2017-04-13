import sys
sys.path.insert(0, 'dwtc-geo-parser')

from reader import *
from databaseOutput import *

import sys

HELP_TEXT = '\033[1mexample.py\033[0m dump'

DB_OUTPUT = 'output.db'

# get arguments
argc = len(sys.argv)
argv = sys.argv

if argc > 1:
	# init db output interface
	db_output = DatabaseOutput(DB_OUTPUT)

	# init reader
	reader = TableReader(argv[1])

	# get first table
	table = reader.get_next_table()

	# insert table into database
	db_output.add_result(dict(), 0, table['url'], dict(), dict(), [], 1, table['relation'])
else:
	print(HELP_TEXT)
