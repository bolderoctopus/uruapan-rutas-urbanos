'''
Autum 2019
Program for reading CSV files and update a SQLite database with their content using the name of the files.

'''

import csv
import math
import sqlite3
from os import listdir



def distance_between(lat1, lng1, lat2, lng2):
    earth_radius = 6371e3
    rad_lat1 = math.radians(float(lat1))
    rad_lat2 = math.radians(float(lat2))
    lat_diff = math.radians(float(lat2) - float(lat1))
    lng_diff = math.radians(float(lng2) - float(lng1))
    
    a = math.sin(lat_diff / 2) * math.sin(lat_diff / 2) + math.cos(rad_lat1) * math.cos(rad_lat2) * math.sin(lng_diff / 2) * math.sin(lng_diff / 2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

    return earth_radius * c

def list_from(csv_file):
    l = []
    for row in csv_file:
        l.append((row["Latitude"], row["Longitude"]))
    return l

def get_values(route_id, points):
    manual_counter = 0
    first_point = ''
    values = []
    for i in range(len(points)):
        distance = distance_between(points[i][0], points[i][1], points[i+1][0], points[i+1][1]) if (i+1)<len(points) else distance_between(points[i][0], points[i][1], points[0][0], points[0][1])
        values.append((route_id, points[i][0], points[i][1], i+1,distance))
    return values

def process_files(cursor):
    csv_files = listdir('rutas')
    for csv_file in csv_files:
        file = open('rutas/' + csv_file, mode='r')
        print('file: ' + 'rutas/' + csv_file)
        csv_reader = csv.DictReader(file)
        short_name = csv_file.split('_')[0]
        cursor.execute("select * from routes where shortName = ?",(short_name, ))

        route_id = cursor.fetchone()[0]
        values = get_values(route_id, list_from(csv_reader))
        update_db(cursor, route_id, values)
        file.close()
        #break


def update_db(cursor, route_id, values):
    cursor.execute('DELETE FROM Points WHERE routeId=?', (str(route_id),))
    cursor.executemany('INSERT INTO Points(routeId, lat, lng, number, distanceToNextPoint) VALUES (?, ?, ?, ?, ?)', values)
    print('route {}. updated, {} points inserted'.format(route_id, len(values)))


conn = sqlite3.connect('pre_packaged_routes.db')
c = conn.cursor()
process_files(c)
conn.commit()
conn.close()