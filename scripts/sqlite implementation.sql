/* finding best start point
rId: 8705
bestStartPoint: ?--95
endPoint int = 42
latitude: 19.422123631224547
longitude: -102.07343347370625
distance: 0.01

*/
drop table if exists bestpoints;
create temp table bestpoints(
	pointId int,
	routeId int,
	lat real,
	lng real,
	number int,
	distanceToNextPoint int,
	wd real,
	rd real,
	betterness real
);

insert into bestpoints
select p1.pointId, p1.routeId, p1.lat, p1.lng,p1.number
	,p1.distanceToNextPoint,
	((19.422123631224547 - p1.lat)*(19.422123631224547 - p1.lat) + (-102.07343347370625 - p1.lng)*(-102.07343347370625 - p1.lng))  [wd],
	(SELECT SUM(p2.distanceToNextPoint) 
		FROM Points p2
		WHERE p2.routeId = 8705
		AND ( ((p1.number > 42) AND (p2.number >= p1.number OR p2.number <= 42)) 
		   OR ((p1.number < 42) AND (p2.number BETWEEN p1.number AND 42)))
	) [rd],
	null
from Points p1
where p1.routeId = 8705
AND p1.lat between (19.422123631224547-0.01) and (19.422123631224547+0.01)
AND p1.lng between (-102.07343347370625-0.01) and (-102.07343347370625+0.01);

update bestpoints
set betterness = ((((select min(wd) from bestpoints)/wd)*0.3) + ((((select min(rd) from bestpoints)/rd)*0.7)));

select *, ((select min(wd) from bestpoints)/wd) [minWd/wd], ((select min(rd) from bestpoints)/rd) [minRd/rd] from bestpoints order by betterness desc 