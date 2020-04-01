/*
Constants
METER_IN_ANGULAR_LAT_LNG = 0.000009229349583
WD_WEIGHT = 100
*/


/*
Pasos para encontrar el mejor startPoint para una ruta dada:
startLocation: par de coordenadas desde donde el usuario comienza
destinationLocation: par de coordenadas hacia donde el usuario se dirige
startPoint: punto que forma parte de la ruta, representa donde el usuario toma la ruta
endPoint: punto que forma parte de la ruta, representa donde el usuario deja la ruta
wd: (walking distance) distancia al cuadrado entre la start/destinationLocation y un punto en una ruta, mientras sea menor mejor, no esta dada en metros si no... angulos?
rd: (route distance) es la suma de la columna distanceToNextPoint (esta columna si esta en metros) para el conjunto de puntos entre start y end Point. Tiene en consideracion el sentido de la ruta, por lo que solo se suman los puntos siguientes hasta llegar al endPoint, mientas sea menor mejor
start/destination Squares: cuadrado alrededor de start/destination Location con una distancia de walkDistLimit entre su centro y sus lados

rT: (route distance total) la distancia total de la ruta, rd al cuadrado  mas wd multiplicada por factor de importacia WD_WEIGHT 

debido a que no siempre el punto con menor wd es el mejor, a veces el usuario puede usar otro start point con mayor wd pero con una rd considerablemente menor

1. Asignar endPoint a cualquier punto dentro d destinationSquare
2. Juntar todos los puntos dentro de startSquare con su wd hacia startLocation y rd con endPoint multiplicada por METER_IN_ANGULAR_LAT_LNG
3. Para cada punto calcular rT = (rd * rd) + (wd * WD_WEIGHT)
4. Escoger como startPoint el punto con menor rT
*/

/* finding best start point
ruta 15
rId: 8702
walkDistLimit: 0.0055376097498
origen: 19.40166, -102.05550
desstino: 19.43052, -102.05597

startPoint: 149
endPoint: 433

WD_WEIGHT = 0.3
RD_WEIGHT = 0.7

bestStartPoint:
bestEndPoint:


*/

-- encontrando bestStartPoint con endPoint y origen
delete from bestPoints;

INSERT INTO bestPoints  
SELECT p1.pointId, p1.routeId, p1.lat, p1.lng, p1.number, p1.distanceToNextPoint,  
	((19.40166 - p1.lat)*(19.40166 - p1.lat) + (-102.05550 - p1.lng)*(-102.05550 - p1.lng))  [wd],  
	(SELECT SUM(p2.distanceToNextPoint)   
	FROM Points p2  
	WHERE p2.routeId = 8702  
	AND ( ((p1.number > 433) AND (p2.number >= p1.number OR p2.number <= 433))   
		OR ((p1.number < 433) AND (p2.number BETWEEN p1.number AND 433)))  
	) * 0.000009229349583 [rd],  
null  
FROM Points p1  
WHERE p1.routeId = 8702  
AND p1.lat BETWEEN (19.40166 - 0.0055376097498) AND (19.40166 + 0.0055376097498)  
AND p1.lng BETWEEN (-102.05550 - 0.0055376097498) AND (-102.05550 + 0.0055376097498);

UPDATE bestPoints SET betterness = (rd * rd) + (wd * 100);

SELECT pointId, routeId, lat, lng, number, distanceToNextPoint  FROM bestPoints ORDER BY betterness ASC LIMIT 1;
-- resultado bestStartPoint: 309


-- encontrando bestEndPoint con startPoint y destino
delete from bestPoints;

INSERT INTO bestPoints 
SELECT p1.pointId, p1.routeId, p1.lat, p1.lng, p1.number, p1.distanceToNextPoint, 
((19.43052 - p1.lat)*(19.43052 - p1.lat) + (-102.05597 - p1.lng)*(-102.05597 - p1.lng))  [wd], 
(SELECT SUM(p2.distanceToNextPoint)  
FROM Points p2 
WHERE p2.routeId = 8702 
AND ( ((149 > p1.number) AND (p2.number >= 149 OR p2.number <= p1.number))  
   OR ((149 < p1.number) AND (p2.number BETWEEN 149 AND p1.number))) 
)  * 0.000009229349583 [rd],  
null 
FROM Points p1 
WHERE p1.routeId = 8702 
AND p1.lat BETWEEN (19.43052 -  0.0055376097498) AND (19.43052 +  0.0055376097498) 
AND p1.lng BETWEEN (-102.05597 -  0.0055376097498) AND (-102.05597 +  0.0055376097498);

UPDATE bestPoints SET betterness = (rd * rd) + (wd * 100);

SELECT pointId, routeId, lat, lng, number, distanceToNextPoint  FROM bestPoints ORDER BY betterness ASC LIMIT 1;
-- resultado bestEndPoint: 433