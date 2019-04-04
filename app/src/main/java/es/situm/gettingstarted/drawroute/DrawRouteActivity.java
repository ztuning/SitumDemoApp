package es.situm.gettingstarted.drawroute;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import es.situm.gettingstarted.R;
import es.situm.gettingstarted.drawpois.GetPoisUseCase;
import es.situm.gettingstarted.guideinstructions.GetBuildingsCaseUse;
import es.situm.sdk.SitumSdk;
import es.situm.sdk.directions.DirectionsRequest;
import es.situm.sdk.error.Error;
import es.situm.sdk.location.util.CoordinateConverter;
import es.situm.sdk.model.cartography.Building;
import es.situm.sdk.model.cartography.Floor;
import es.situm.sdk.model.cartography.Point;
import es.situm.sdk.model.directions.Route;
import es.situm.sdk.model.directions.RouteSegment;
import es.situm.sdk.model.location.Bounds;
import es.situm.sdk.model.location.CartesianCoordinate;
import es.situm.sdk.model.location.Coordinate;
import es.situm.sdk.utils.Handler;

/**
 * Created by alberto.penas on 10/07/17.
 */

public class DrawRouteActivity
        extends AppCompatActivity
        implements OnMapReadyCallback {

    private final String TAG = getClass().getSimpleName();
    private GetPoisUseCase getPoisUseCase = new GetPoisUseCase();
    private GetBuildingsCaseUse getBuildingsUseCase = new GetBuildingsCaseUse();
    private ProgressBar progressBar;
    private GoogleMap googleMap;
    private Building currentBuilding;
    private List<Polyline> polylines = new ArrayList<>();
    private Marker markerDestination,markerOrigin;
    private String  buildingId,floorId;
    private Point pointOrigin,pointDestination;
    private CoordinateConverter coordinateConverter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_route);
        Intent intent = getIntent();
        getSupportActionBar().setSubtitle(R.string.tv_select_points);
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
                buildingId = intent.getStringExtra(Intent.EXTRA_TEXT);
        }
        setup();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onDestroy() {
        getPoisUseCase.cancel();
        getBuildingsUseCase.cancel();
        SitumSdk.navigationManager().removeUpdates();
        super.onDestroy();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;
        getBuildingsUseCase.get(buildingId, new GetBuildingsCaseUse.Callback() {
            @Override
            public void onSuccess(Building building, Floor floor, Bitmap bitmap) {
                progressBar.setVisibility(View.GONE);
                floorId = floor.getIdentifier();
                currentBuilding = building;
                coordinateConverter = new CoordinateConverter(building.getDimensions(),building.getCenter(),building.getRotation());
                drawBuilding(building, bitmap);
            }

            @Override
            public void onError(Error error) {
                Toast.makeText(DrawRouteActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        this.googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (pointOrigin == null) {
                    if(markerOrigin!=null){
                        clearMap();
                    }
                    markerOrigin = googleMap.addMarker(new MarkerOptions().position(latLng).title("origin"));
                    pointOrigin = createPoint(latLng);
                }else {
                    markerDestination = googleMap.addMarker(new MarkerOptions().position(latLng).title("destination"));
                    calculateRoute(latLng);
                }


            }
        });

    }

    private void calculateRoute(LatLng latLng) {
        Point pointDestination = createPoint(latLng);
        DirectionsRequest directionsRequest = new DirectionsRequest.Builder()
                .from(pointOrigin, null)
                .to(pointDestination)
                .build();
        SitumSdk.directionsManager().requestDirections(directionsRequest, new Handler<Route>() {
            @Override
            public void onSuccess(Route route) {
                drawRoute(route);
                centerCamera(route);
                hideProgress();
                pointOrigin = null;

            }

            @Override
            public void onFailure(Error error) {
                hideProgress();
                Toast.makeText(DrawRouteActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearMap(){
        markerOrigin.remove();
        markerDestination.remove();
        removePolylines();
    }
    private Point createPoint(LatLng latLng) {
        Coordinate c = new Coordinate(latLng.latitude, latLng.longitude);
        CartesianCoordinate cc= coordinateConverter.toCartesianCoordinate(c);
        Point p = new Point(buildingId,floorId,c,cc );
        return p;
    }

    private void removePolylines() {
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines.clear();
    }

    private void drawRoute(Route route) {
        for (RouteSegment segment : route.getSegments()) {
            //For each segment you must draw a polyline
            //Add an if to filter and draw only the current selected floor
            List<LatLng> latLngs = new ArrayList<>();
            for (Point point : segment.getPoints()) {
                latLngs.add(new LatLng(point.getCoordinate().getLatitude(), point.getCoordinate().getLongitude()));
            }

            PolylineOptions polyLineOptions = new PolylineOptions()
                    .color(Color.GREEN)
                    .width(4f)
                    .addAll(latLngs);
            polylines.add(googleMap.addPolyline(polyLineOptions));

        }
    }
    void drawBuilding(Building building, Bitmap bitmap){
        Bounds drawBounds = building.getBounds();
        Coordinate coordinateNE = drawBounds.getNorthEast();
        Coordinate coordinateSW = drawBounds.getSouthWest();
        LatLngBounds latLngBounds = new LatLngBounds(
                new LatLng(coordinateSW.getLatitude(), coordinateSW.getLongitude()),
                new LatLng(coordinateNE.getLatitude(), coordinateNE.getLongitude()));

        this.googleMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                .bearing((float) building.getRotation().degrees())
                .positionFromBounds(latLngBounds));

        this.googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
    }

    private void centerCamera(Route route) {
        Coordinate from = route.getFrom().getCoordinate();
        Coordinate to = route.getTo().getCoordinate();

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(new LatLng(from.getLatitude(), from.getLongitude()))
                .include(new LatLng(to.getLatitude(), to.getLongitude()));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }

    private void setup() {
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void hideProgress(){
        progressBar.setVisibility(View.GONE);
    }
}
