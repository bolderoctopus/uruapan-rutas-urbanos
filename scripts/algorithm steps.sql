/*Steps for finding the best start point for a given route:
wd: squared distance from the start/destination location to a point's location, the lesser the better
rd: distance from start to end point, it equals the sum of the distanceToNextPoint column of the following points until the end point, the lesser the better
start/end Points: point inside the respective region, part of a route circuit, represent where the user would be steping in the route
start/destination Squares: region with the start/destination point in its center, and its side are walkDistLimit*2 long

1. find an end point, any point within the destinationSquare is good
2. get all the points inside the startSquare with their respective wd and rd with the given endPoint,these will be the startPoints
3. find the minimun wd and rd, these will be minWd and minRd
4. for each of the startPoints, calculate (((minWd/wd)* 0.3) + ((@minRd/rd) * 0.7)), the closer to 1, the more optimum is the route 
	(if the factors dont add up to 1, then the higher should still be the better)
	0.3 and 0.7 are factors for deciding what sould be more important when trying to calculate how optimum is the route
*/


/*
find end point
create temp table
fill temp table with startPoints with their wd, rd
find minWd, minRd from temp table
update temp table with every points corresponding score
select the point the max score 
*/


--points near origin
declare @rId int = 8705
,@startPoint int = null
,@endPoint int = 42
,@distance float = 0.01
-- origin latlng
,@latitude float = 19.422123631224547
,@longitude float = -102.07343347370625
,@minWd float = 1.46756100628329E-05
,@minRd float = 1130

;with originPoints (pointId, lat,lng, point#, wd, rd)
as(
SELECT p1.pointId, p1.lat, p1.lng, p1.number [point#],
		((@latitude - p1.lat)*(@latitude - p1.lat) + (@longitude - p1.lng)*(@longitude - p1.lng)) [wd],
		(SELECT SUM(p2.distanceToNextPoint) 
		FROM Points p2
		WHERE p2.routeId = @rId 
		AND ( ((p1.number > @endPoint) AND (p2.number >= p1.number OR p2.number <= @endPoint)) 
		   OR ((p1.number < @endPoint) AND (p2.number BETWEEN p1.number AND @endPoint)))
		) [rd]


FROM Points p1
WHERE p1.routeId = @rId  
AND p1.lat BETWEEN (19.4121236312245) AND (19.4321236312245)
AND p1.lng BETWEEN (-102.083433473706) AND (-102.063433473706)
)
--ORDER BY [betterness]

select *, @minWd/wd [minWd/wd], @minRd/ rd [@minRd/rd], 
		
		(((@minWd/wd)* 0.3) + ((@minRd/rd) * 0.7)) [betterness]

from originPoints
order by [betterness] desc


/*
start = 70
betterStart = 95
*/

/*
small todo list:
3. attempt to improve minWd/wd with something more like, the relative difference againts the min(best)
*/




/*
modified for endPoint

--points near destination
declare @rId int = 8705
,@startPoint int = 70
,@endPoint int = null
,@distance float = 0.01
-- destination latlng
,@latitude float = 19.41543181604419
,@longitude float = -102.03406568616629
,@minWd float = 1.48314739378978E-06
,@minRd float = 510

;with destinationPoints (pointId, lat,lng, point#, wd, rd)
as(
SELECT p1.pointId, p1.lat, p1.lng, p1.number [point#],
		((@latitude - p1.lat)*(@latitude - p1.lat) + (@longitude - p1.lng)*(@longitude - p1.lng)) [wd],
		(SELECT SUM(p2.distanceToNextPoint) 
		FROM Points p2
		WHERE p2.routeId = @rId 
		AND ( ((@startPoint > p1.number) AND (p2.number >= @startPoint OR p2.number <= p1.number)) 
		   OR ((@startPoint < p1.number) AND (p2.number BETWEEN @startPoint AND p1.number)))
		) [rd]


FROM Points p1
WHERE p1.routeId = @rId  
AND p1.lat BETWEEN (@latitude - @distance) AND (@latitude + @distance)
AND p1.lng BETWEEN (@longitude - @distance) AND (@longitude + @distance)
)

select *, @minWd/wd [minWd/wd], @minRd/ rd [@minRd/rd], 
		
		(((@minWd/wd)* 0.3) + ((@minRd/rd) * 0.7)) [betterness]

from destinationPoints
order by [betterness] desc

*/