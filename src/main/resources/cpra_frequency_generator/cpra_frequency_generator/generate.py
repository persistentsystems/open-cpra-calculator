#
# This Source Code Form is subject to the terms of the Mozilla Public License, v.
# 2.0 with a Healthcare Disclaimer.
#
# A copy of the Mozilla Public License, v. 2.0 with the Healthcare Disclaimer can
# be found under the top level directory, named LICENSE.
#
# If a copy of the MPL was not distributed with this file, You can obtain one at
# http://mozilla.org/MPL/2.0/.
#
# If a copy of the Healthcare Disclaimer was not distributed with this file, You
# can obtain one at the project website https://github.com/persistentsystems/open-cpra-calculator.
#
# Copyright (C) 2016-2018 Persistent Systems, Inc.
#

#
# Generates CSVs which can be used by the cpra web service for custom population frequencies
# 
# William Gordon - wjgordon@partners.org
#

import csv, operator, os, argparse

from jinja2 import Template, Environment, FileSystemLoader
from collections import defaultdict

PATH = os.path.dirname(os.path.abspath(__file__))

TEMPLATE_ENVIRONMENT = Environment(
	autoescape=False,
	loader=FileSystemLoader(os.path.join(PATH, 'templates')),
	trim_blocks=False)

parser = argparse.ArgumentParser(description='Generates files which can be used by the cpra web service for custom population frequencies.')
parser.add_argument('--input_file', help='input frequency table file', required=True)
parser.add_argument('--freq_name', help='name of frequency table', required=True)
args = parser.parse_args()

def render_template(template_filename, context):
	return TEMPLATE_ENVIRONMENT.get_template(template_filename).render(context)
 
def create_index_file(frequencies):
	freq_name = args.freq_name
	ethnicities = 'Default'
	ethnicity_frequencies = 1.00

	context = {
		'freq_name': freq_name,
		'ethnicities': ethnicities,
		'ethnicity_frequencies': ethnicity_frequencies,
		'frequencies': frequencies
	}

	# Output frequency csv
	with open(args.freq_name + '_freq.csv', 'w') as f:
		file_out = render_template('template_freq.csv', context)
		f.write(file_out.encode('utf-8').strip())

 	# Output meta csv
 	with open(args.freq_name + '_meta.csv', 'w') as f:
		file_out = render_template('template_meta.csv', context)
		f.write(file_out.encode('utf-8').strip())
	print("Done")


 
def generate_frequencies():
	denominator = 0
	frequencies = defaultdict(int)
	frequencies_set = defaultdict(int)

	with open(args.input_file, 'r') as csvfile:
	
		reader = csv.DictReader(csvfile)

		for row in reader:
			
			tmp_set = set()

			denominator+=1
			antigen_a = set()
			antigen_a.add('A' + row['A1'])
			antigen_a.add('A' + row['A2'])

			while 'A' in antigen_a: antigen_a.remove('A')

			antigen_b = set()
			antigen_b.add('B' + row['B1'])
			antigen_b.add('B' + row['B2'])
			while 'B' in antigen_b: antigen_b.remove('B')

			tmp_set = frozenset(antigen_a.union(antigen_b))
	
			frequencies_set[tmp_set] += 1

	for antigens, count in frequencies_set.iteritems():
		converted_antigens = ';'.join(antigens)
		frequencies[converted_antigens] = [converted_antigens, count, format(count/float(denominator), '.24f'), denominator]
	return(frequencies)

def main():
	create_index_file(generate_frequencies())
 
########################################
 
if __name__ == "__main__":
	print("Generating frequency files...")
	main()