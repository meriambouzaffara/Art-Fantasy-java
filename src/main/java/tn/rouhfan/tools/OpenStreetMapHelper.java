package tn.rouhfan.tools;

import tn.rouhfan.entities.Magasin;

import java.util.List;
import java.util.stream.Collectors;

public final class OpenStreetMapHelper {

    public static final double DEFAULT_LATITUDE = 36.8065;
    public static final double DEFAULT_LONGITUDE = 10.1815;

    private OpenStreetMapHelper() {
    }

    public static boolean hasCoordinates(Magasin magasin) {
        return magasin != null && magasin.getLatitude() != null && magasin.getLongitude() != null
                && isValidLatitude(magasin.getLatitude()) && isValidLongitude(magasin.getLongitude());
    }

    public static boolean isValidLatitude(double latitude) {
        return latitude >= -90 && latitude <= 90;
    }

    public static boolean isValidLongitude(double longitude) {
        return longitude >= -180 && longitude <= 180;
    }

    public static String buildPickerMap(Double latitude, Double longitude) {
        double lat = latitude != null && isValidLatitude(latitude) ? latitude : DEFAULT_LATITUDE;
        double lon = longitude != null && isValidLongitude(longitude) ? longitude : DEFAULT_LONGITUDE;
        boolean hasMarker = latitude != null && longitude != null && isValidLatitude(latitude) && isValidLongitude(longitude);
        return buildMapHtml(lat, lon, hasMarker, true, List.of());
    }

    public static String buildDisplayMap(Magasin magasin) {
        double lat = hasCoordinates(magasin) ? magasin.getLatitude() : DEFAULT_LATITUDE;
        double lon = hasCoordinates(magasin) ? magasin.getLongitude() : DEFAULT_LONGITUDE;
        return buildMapHtml(lat, lon, false, false, hasCoordinates(magasin) ? List.of(magasin) : List.of());
    }

    /**
     * Construit une carte HTML avec calcul d'itinéraire.
     * L'utilisateur saisit son adresse de départ, l'itinéraire est calculé via OSRM/Nominatim.
     *
     * @param destLat  latitude destination (magasin)
     * @param destLon  longitude destination (magasin)
     * @param destName nom affiché pour la destination
     */
    public static String buildRouteMap(double destLat, double destLon, String destName) {
        String esc = escapeJs(destName);
        return "<!DOCTYPE html>\n"
                + "<html><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\"/>"
                + "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet-routing-machine@3.2.12/dist/leaflet-routing-machine.css\"/>"
                + "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>"
                + "<script src=\"https://unpkg.com/leaflet-routing-machine@3.2.12/dist/leaflet-routing-machine.min.js\"></script>"
                + "<style>"
                + "html,body,#map{height:100%;width:100%;margin:0;padding:0;overflow:hidden;"
                + "font-family:Segoe UI,Arial,sans-serif;background:#f8f7fb;}"
                + "#ctrl{position:absolute;z-index:999;top:10px;left:50%;transform:translateX(-50%);"
                + "background:white;padding:10px 14px;border-radius:14px;"
                + "box-shadow:0 4px 18px rgba(36,17,151,0.18);display:flex;gap:8px;align-items:center;"
                + "max-width:92vw;}"
                + "#adr{padding:7px 12px;border:1.5px solid #241197;border-radius:8px;"
                + "font-size:13px;width:240px;outline:none;color:#241197;}"
                + "#btn{padding:7px 14px;background:#241197;color:white;border:none;"
                + "border-radius:8px;font-weight:bold;cursor:pointer;font-size:13px;white-space:nowrap;"
                + "transition:background .2s;}"
                + "#btn:hover{background:#6c2a90;}"
                + "#msg{position:absolute;z-index:998;bottom:12px;left:50%;transform:translateX(-50%);"
                + "background:white;padding:7px 16px;border-radius:10px;font-size:12px;color:#241197;"
                + "box-shadow:0 2px 8px rgba(36,17,151,0.14);display:none;}"
                + ".leaflet-routing-container{display:none!important;}"
                + "</style>"
                + "</head><body>"
                + "<div id=\"ctrl\">"
                + "<input id=\"adr\" type=\"text\" placeholder=\"Votre adresse de d&eacute;part...\" "
                + "onkeydown=\"if(event.key==='Enter')go();\"/>"
                + "<button id=\"btn\" onclick=\"go()\">&#x1F5FA; Itin&eacute;raire</button>"
                + "</div>"
                + "<div id=\"msg\"></div>"
                + "<div id=\"map\"></div>"
                + "<script>"
                + "var dLat=" + destLat + ",dLon=" + destLon + ",dName='" + esc + "';"
                + "var map=L.map('map').setView([dLat,dLon],14);"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',"
                + "{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(map);"
                + "var destIcon=L.marker([dLat,dLon]).addTo(map).bindPopup('<b>&#x1F3EA; '+dName+'</b>').openPopup();"
                + "var rc=null;"
                + "function showMsg(txt,color){"
                + "var m=document.getElementById('msg');"
                + "m.style.display='block';m.style.color=color||'#241197';m.textContent=txt;"
                + "setTimeout(function(){m.style.display='none';},3500);}"
                + "function go(){"
                + "var a=document.getElementById('adr').value.trim();"
                + "if(!a){showMsg('Entrez une adresse de depart.','#d63031');return;}"
                + "document.getElementById('btn').textContent='⏳ Calcul...';"
                + "fetch('https://nominatim.openstreetmap.org/search?format=json&limit=1&q='"
                + "+encodeURIComponent(a))"
                + ".then(function(r){return r.json();})"
                + ".then(function(d){"
                + "document.getElementById('btn').textContent='\\uD83D\\uDDFA Itineraire';"
                + "if(!d||!d.length){showMsg('Adresse introuvable. Precisez la ville.','#d63031');return;}"
                + "var fLat=parseFloat(d[0].lat),fLon=parseFloat(d[0].lon);"
                + "if(rc){try{map.removeControl(rc);}catch(e){}}"
                + "rc=L.Routing.control({"
                + "waypoints:[L.latLng(fLat,fLon),L.latLng(dLat,dLon)],"
                + "routeWhileDragging:false,"
                + "showAlternatives:false,"
                + "show:false,"
                + "lineOptions:{styles:[{color:'#241197',weight:5,opacity:0.85}]},"
                + "createMarker:function(i,wp){"
                + "return L.marker(wp.latLng).bindPopup(i===0"
                + "?'<b>&#x1F4CD; Votre depart</b>'"
                + ":'<b>&#x1F3EA; '+dName+'</b>');}"
                + "}).addTo(map);"
                + "rc.on('routesfound',function(e){"
                + "var r=e.routes[0];var km=(r.summary.totalDistance/1000).toFixed(1);"
                + "var min=Math.round(r.summary.totalTime/60);"
                + "showMsg('Distance : '+km+' km  —  Temps estimé : '+min+' min','#241197');"
                + "map.fitBounds(L.latLngBounds([L.latLng(fLat,fLon),L.latLng(dLat,dLon)]).pad(0.2));});"
                + "rc.on('routingerror',function(){showMsg('Itineraire indisponible pour ce trajet.','#d63031');});"
                + "}).catch(function(){"
                + "document.getElementById('btn').textContent='\\uD83D\\uDDFA Itineraire';"
                + "showMsg('Erreur reseau. Verifiez votre connexion.','#d63031');});}"
                + "setTimeout(function(){map.invalidateSize();},80);"
                + "setTimeout(function(){map.invalidateSize();},300);"
                + "</script></body></html>";
    }

    public static String buildDisplayMap(List<Magasin> magasins) {
        List<Magasin> positioned = magasins == null ? List.of()
                : magasins.stream().filter(OpenStreetMapHelper::hasCoordinates).collect(Collectors.toList());
        double lat = positioned.isEmpty() ? DEFAULT_LATITUDE : positioned.get(0).getLatitude();
        double lon = positioned.isEmpty() ? DEFAULT_LONGITUDE : positioned.get(0).getLongitude();
        return buildMapHtml(lat, lon, false, false, positioned);
    }

    private static String buildMapHtml(double lat, double lon, boolean hasMarker, boolean editable, List<Magasin> magasins) {
        StringBuilder markers = new StringBuilder();
        for (Magasin magasin : magasins) {
            if (!hasCoordinates(magasin)) {
                continue;
            }
            markers.append("{lat:")
                    .append(magasin.getLatitude())
                    .append(",lon:")
                    .append(magasin.getLongitude())
                    .append(",name:'")
                    .append(escapeJs(magasin.getNom()))
                    .append("',address:'")
                    .append(escapeJs(magasin.getAdresse()))
                    .append("'},");
        }

        return "<!DOCTYPE html>\n"
                + "<html><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\"/>"
                + "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>"
                + "<style>"
                + "html,body,#map{height:100%;width:100%;margin:0;padding:0;overflow:hidden;background:#f8f7fb;font-family:Segoe UI,Arial,sans-serif;}"
                + ".leaflet-popup-content{font-size:13px;}"
                + ".hint{position:absolute;z-index:999;left:10px;bottom:10px;background:white;padding:7px 10px;"
                + "border-radius:8px;box-shadow:0 3px 12px rgba(0,0,0,.16);font-size:12px;color:#241197;font-weight:600;}"
                + "</style></head><body><div id=\"map\"></div>"
                + (editable ? "<div class=\"hint\">Cliquez sur la carte pour choisir la position</div>" : "")
                + "<script>"
                + "var map=L.map('map',{zoomControl:true}).setView([" + lat + "," + lon + "]," + (hasMarker ? "15" : "11") + ");"
                + "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,"
                + "attribution:'&copy; OpenStreetMap'}).addTo(map);"
                + "var editable=" + editable + ";"
                + "var marker=null;"
                + "function notify(lat,lon){if(window.javaConnector&&window.javaConnector.positionSelected){window.javaConnector.positionSelected(lat,lon);}}"
                + "function setMarker(lat,lon,label){"
                + "if(marker){marker.setLatLng([lat,lon]);}else{marker=L.marker([lat,lon],{draggable:editable}).addTo(map);"
                + "if(editable){marker.on('dragend',function(e){var p=e.target.getLatLng();notify(p.lat,p.lng);});}}"
                + "marker.bindPopup(label||'Position magasin').openPopup();map.setView([lat,lon],15);}"
                + (hasMarker ? "setMarker(" + lat + "," + lon + ",'Position magasin');" : "")
                + "var markers=[" + markers + "];"
                + "var group=[];"
                + "markers.forEach(function(m){var mk=L.marker([m.lat,m.lon]).addTo(map).bindPopup('<b>'+m.name+'</b><br>'+m.address);group.push(mk);});"
                + "if(group.length===1){map.setView(group[0].getLatLng(),15);group[0].openPopup();}"
                + "if(group.length>1){map.fitBounds(L.featureGroup(group).getBounds().pad(0.25));}"
                + "if(editable){map.on('click',function(e){setMarker(e.latlng.lat,e.latlng.lng,'Position selectionnee');notify(e.latlng.lat,e.latlng.lng);});}"
                + "setTimeout(function(){map.invalidateSize();},80);setTimeout(function(){map.invalidateSize();},260);"
                + "</script></body></html>";
    }

    private static String escapeJs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
