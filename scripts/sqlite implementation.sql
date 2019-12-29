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

-- walkDistLimit esta en metros * metro_ang
-- distanceToNextPoint esta en metros?

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

UPDATE bestPoints SET betterness = ((((SELECT MIN(wd) FROM bestPoints)/wd)*0.3) + ((((SELECT MIN(rd) FROM bestPoints)/rd)*0.7)));

SELECT pointId, routeId, lat, lng, number, distanceToNextPoint  FROM bestPoints ORDER BY betterness DESC LIMIT 1;
-- resultado bestStartPoint: 345
-- deberia ser 309, 310


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
) [rd], 
null 
FROM Points p1 
WHERE p1.routeId = 8702 
AND p1.lat BETWEEN (19.43052 -  0.0055376097498) AND (19.43052 +  0.0055376097498) 
AND p1.lng BETWEEN (-102.05597 -  0.0055376097498) AND (-102.05597 +  0.0055376097498);

UPDATE bestPoints SET betterness = ((((SELECT MIN(wd) FROM bestPoints)/wd)*0.3) + ((((SELECT MIN(rd) FROM bestPoints)/rd)*0.7)));

SELECT pointId, routeId, lat, lng, number, distanceToNextPoint  FROM bestPoints ORDER BY betterness DESC LIMIT 1;
-- resultado bestEndPoint: 433