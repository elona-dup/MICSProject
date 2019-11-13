package uni.lu.mics.mics_project.nmbd;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.lang3.text.WordUtils;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

import uni.lu.mics.mics_project.nmbd.app.AppGlobalState;
import uni.lu.mics.mics_project.nmbd.app.service.Images.ImageViewUtils;
import uni.lu.mics.mics_project.nmbd.domain.model.Event;
import uni.lu.mics.mics_project.nmbd.domain.model.User;
import uni.lu.mics.mics_project.nmbd.infra.repository.EventRepository;
import uni.lu.mics.mics_project.nmbd.infra.repository.RepoCallback;
import uni.lu.mics.mics_project.nmbd.infra.repository.UserRepository;

public class EventActivity extends AppCompatActivity {

    final String TAG = "EventActivity";

    AppGlobalState globalState;
    EventRepository eventRepo;
    UserRepository userRepo;

    private ImageButton imgView;
    private TextView eventTitle;
    private TextView dateField;
    private TextView place;
    private TextView eventType;
    private TextView host;
    private TextView numberOfParticipants;
    private TextView descriptionBox;
    private Button joinLeaveBtn;
    private Button editBtn;
    private User currentUser;
    private String currentUserID;
    private Event currentEvent;

    private ImageView hostProfileImgView;

    //Open Map variables
    MapView map = null;
    //Open Map variables
    IMapController mapController;
    GeoPoint startPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        setContentView(R.layout.activity_event);

        globalState = (AppGlobalState) getApplicationContext();
        eventRepo = globalState.getRepoFacade().eventRepo();
        userRepo = globalState.getRepoFacade().userRepo();
        imgView = findViewById(R.id.eventImageBtn);
        numberOfParticipants = findViewById(R.id.number_peeps_going);
//        Retrieve Intent and get current user and event
        Intent intent = getIntent();
        currentUser = (User) intent.getSerializableExtra("currentUser");
        currentUserID = currentUser.getId();
        currentEvent = (Event) intent.getSerializableExtra("currentEvent");
//        Check if there is an image uri,this would happen if the event was just created and the picture is still being
//        uploaded
//        , otherwise display the picture from the eventID.
//        imgView: is the imageView object where you want the picture to be displayed

        setJoinLeaveButton();
        setEditButton();

        if (intent.hasExtra("image")) {
            Uri imageUri = Uri.parse(intent.getStringExtra("image"));

            ImageViewUtils.displayPicUri(this, imageUri, imgView);
        } else {
            ImageViewUtils.displayEventPicID(this, currentEvent.getId(), imgView);
        }
        setEventName(currentEvent.getName());
        setDate(currentEvent.getDate());
        setAdress(currentEvent.getEventAddress());
        setCategory(currentEvent.getCategory());
        String creatorId = currentEvent.getCreator();
        setProfilePics(creatorId);
        setDescription(currentEvent.getDescription());
        setMap();
        setParticipants();
    }


    private void setProfilePics(String creatorId){
        userRepo.findById(creatorId, new RepoCallback<User>() {
            @Override
            public void onCallback(User user) {
                setHost(user.getName());
                Log.d(TAG, "Setting Event creator's Name");
                hostProfileImgView = findViewById(R.id.hostProfileImgView);
                ImageViewUtils.displayUserCirclePic(EventActivity.this, user,hostProfileImgView );
            }
        });
    }



    private void setMap(){
        //Map variables
        map = findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(false);
        map.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mapController = map.getController();
        mapController.setZoom(10.);
        startPoint = new GeoPoint(currentEvent.getGpsLat(), currentEvent.getGpsLong());
        Log.d(TAG, startPoint.toString());

        Marker m = new Marker(map);
        m.setIcon(getDrawable(R.drawable.map_marker));
        m.setPosition(startPoint);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(m);
        mapController.setCenter(new GeoPoint(startPoint));

        


        //Toast.makeText(EventActivity.this, currentEvent.getId(), Toast.LENGTH_LONG).show();

    }

    private void setEventName(String name) {
        eventTitle = (TextView) findViewById(R.id.eventTitle);
        eventTitle = (TextView) findViewById(R.id.eventTitle);
        eventTitle.setText(name);
    }

    private void setDate(String dateString) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat formatterToStr = new SimpleDateFormat("EEEE, MMMM dd");
        dateField = (TextView) findViewById(R.id.day_monthId);
        try {
            Date date = formatter.parse(dateString);
            dateField.setText(formatterToStr.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    private void setAdress(String adress) {
        place = (TextView) findViewById(R.id.placeId);
        place.setText(adress);
    }

    private void setCategory(String category) {
        eventType = (TextView) findViewById(R.id.categoryId);
        eventType.setText(WordUtils.capitalize(category.toLowerCase()));
    }

    private void setHost(String hostName) {
        host = (TextView) findViewById(R.id.hosted_by);
        host.setText("Hosted by " + hostName);

    }

    

    private void setDescription(String description) {
        descriptionBox = findViewById(R.id.description_box);
        descriptionBox.setMovementMethod(new ScrollingMovementMethod());
        descriptionBox.setText(description);

    }

    private void setJoinLeaveButton() {
        joinLeaveBtn = (Button) findViewById(R.id.participateBtn);
        if (currentEvent.getEventParticipants().contains(currentUser.getId())) {
            joinLeaveBtn.setText("Leave");
        } else {
            joinLeaveBtn.setText("Join");
        }
    }

    private void setEditButton() {
        editBtn = findViewById(R.id.edit_event);
        if (currentEvent.getEventAdmins().contains(currentUser.getId())) {
            editBtn.setVisibility(View.VISIBLE);
        } else {
            editBtn.setVisibility(View.INVISIBLE);
        }
    }

    public void joinLeaveEventOnClick(View view) {
        int numParticipants = currentEvent.getEventParticipants().size();
        if (joinLeaveBtn.getText() == "Join") {
            currentEvent.getEventParticipants().add(currentUser.getId());
            eventRepo.addElement(currentEvent.getId(), "eventParticipants", currentUser.getId());
            joinLeaveBtn.setText("Leave");
            numParticipants++;

        } else {
            currentEvent.getEventParticipants().remove(currentUser.getId());
            eventRepo.removeElement(currentEvent.getId(), "eventParticipants", currentUser.getId());
            joinLeaveBtn.setText("Join");
            numParticipants--;
        }
        if (numParticipants == 1 || numParticipants == 0){
            numberOfParticipants.setText(numParticipants + " person is going");
        } else {
            numberOfParticipants.setText(numParticipants + " people are going");
        }
        eventRepo.findById(currentEvent.getId(), new RepoCallback<Event>() {
            @Override
            public void onCallback(Event model) {
                currentEvent = model;
                setParticipants();
            }
        });

    }
  


    private void setParticipants() {
        List<String> participants = currentEvent.getEventParticipants();
        int numParticipants = participants.size();
        Log.d(TAG, "Number of participants:"+ numParticipants);
        if (numParticipants == 1 || numParticipants == 0 ) {
            numberOfParticipants.setText(numParticipants + " person is going");
        } else {
            numberOfParticipants.setText(numParticipants + " people are going");
        }
        ImageView prof1 = findViewById(R.id.profile1);
        ImageView prof2 = findViewById(R.id.profile2);
        ImageView prof3 = findViewById(R.id.profile3);
        if (numParticipants==0){
          prof1.setVisibility(View.INVISIBLE);
          prof2.setVisibility(View.INVISIBLE);
            prof3.setVisibility(View.INVISIBLE);
        } else if (numParticipants==1){
            prof1.setVisibility(View.VISIBLE);
            prof2.setVisibility(View.INVISIBLE);
            prof3.setVisibility(View.INVISIBLE);
            ImageViewUtils.displayUserCirclePicID(this, participants.get(0), prof1);
        } else if (numParticipants==2){
            prof1.setVisibility(View.VISIBLE);
            prof2.setVisibility(View.VISIBLE);
            prof3.setVisibility(View.INVISIBLE);
            ImageViewUtils.displayUserCirclePicID(this, participants.get(0), prof1);
            ImageViewUtils.displayUserCirclePicID(this, participants.get(1), prof2);
        } else if (numParticipants==3){
            prof1.setVisibility(View.VISIBLE);
            prof2.setVisibility(View.VISIBLE);
            prof3.setVisibility(View.VISIBLE);
            ImageViewUtils.displayUserCirclePicID(this, participants.get(0), prof1);
            ImageViewUtils.displayUserCirclePicID(this, participants.get(1), prof2);
            ImageViewUtils.displayUserCirclePicID(this, participants.get(2), prof3);
        } else {
            Random rand = new Random();
            int r = rand.nextInt(participants.size());
            ImageViewUtils.displayUserCirclePicID(this, participants.get(r), prof1);
            participants.remove(r);
            r = rand.nextInt(participants.size());
            ImageViewUtils.displayUserCirclePicID(this, participants.get(r), prof2);
            participants.remove(r);
            r = rand.nextInt(participants.size());
            ImageViewUtils.displayUserCirclePicID(this, participants.get(r), prof3);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed Called");
        Intent intent = new Intent(this, HomepageActivity.class);
        intent.putExtra("currentUser", currentUser);
        startActivity(intent);
    }

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }


}


