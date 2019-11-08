package uni.lu.mics.mics_project.nmbd;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import uni.lu.mics.mics_project.nmbd.adapters.AdapterCallBack;
import uni.lu.mics.mics_project.nmbd.adapters.EventListAdapter;
import uni.lu.mics.mics_project.nmbd.adapters.FriendListAdapter;
import uni.lu.mics.mics_project.nmbd.adapters.FriendRequestListAdapter;
import uni.lu.mics.mics_project.nmbd.adapters.FriendSearchListAdapter;
import uni.lu.mics.mics_project.nmbd.app.AppGlobalState;
import uni.lu.mics.mics_project.nmbd.app.service.Authentification;
import uni.lu.mics.mics_project.nmbd.app.service.ExtendedListEvent;
import uni.lu.mics.mics_project.nmbd.app.service.ExtendedListHash;
import uni.lu.mics.mics_project.nmbd.app.service.Storage;
import uni.lu.mics.mics_project.nmbd.app.service.StorageCallback;
import uni.lu.mics.mics_project.nmbd.domain.model.Event;
import uni.lu.mics.mics_project.nmbd.domain.model.User;
import uni.lu.mics.mics_project.nmbd.infra.repository.EventRepository;
import uni.lu.mics.mics_project.nmbd.infra.repository.RepoCallback;
import uni.lu.mics.mics_project.nmbd.infra.repository.RepoMultiCallback;
import uni.lu.mics.mics_project.nmbd.infra.repository.UserRepository;

public class ViewEventsActivity extends AppCompatActivity {

    private final String TAG = "ViewEventsActivity";

    //Friend search results
    private final ExtendedListEvent searchResultList = new ExtendedListEvent();
    //Friend Requests received
    private final ExtendedListEvent eventInviteList = new ExtendedListEvent();
    //Friend List
    private final ExtendedListEvent eventAttList = new ExtendedListEvent();

    AppGlobalState globalState;
    UserRepository userRepo;
    Authentification authService;
    Storage storageService;
    EventRepository eventRepo;

    User currentUser;
    private String currentUserID;
    //View variables
    private RecyclerView mSearchResultRecyclerView;
    private EventListAdapter mEventSearchListAdapter;
    private EditText searchEdit;
    private RecyclerView mEventInviteListRecyclerView;
    private EventListAdapter mEventInviteListAdapter;
    private TextView eventInviteLabel;
    private RecyclerView mEventAttRecyclerView;
    private EventListAdapter mEventAttListAdapter;
    private TextView eventAttLabel;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_events);
        //Initialize the repos and services
        globalState = (AppGlobalState) getApplicationContext();
        userRepo = globalState.getRepoFacade().userRepo();
        eventRepo = globalState.getRepoFacade().eventRepo();
        authService = globalState.getServiceFacade().authentificationService();
        storageService = globalState.getServiceFacade().storageService();
        //Get intent from previous Activity and retrieve and initialize the current user
        Intent intent = getIntent();
        currentUser = (User) intent.getSerializableExtra("currentUser");
        currentUserID = currentUser.getId();

        //Initialize the different views in layout
        searchEdit = findViewById(R.id.search_edit);
        eventInviteLabel = findViewById(R.id.invitation_pending_label);
        eventAttLabel = findViewById(R.id.events_list_label);
        //Attaching the recycler views to their corresponding View items
        mSearchResultRecyclerView = findViewById(R.id.search_result_recyclerview);
        mEventInviteListRecyclerView = findViewById(R.id.invitation_pending_recyclerview);
        mEventAttRecyclerView = findViewById(R.id.events_recyclerview);

        //initialize the friend search recyclerView, friend request recycler view
        initializeSearchRecyclerView();
        initializeEventsRecyclerView();
        initializeEventsInviteRecyclerView();

        //Add listener to search edit view so Recycler view can be updated when there is a change
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, s.toString() + " is being searched");
                //If the text in the search edit view has changed and the it is not an empty string
                if (s.length() != 0) {
                    //Delete all the elements in the searchResultList to make space for the new results of the search
                    searchResultList.clearLists();
                    mEventSearchListAdapter.notifyDataSetChanged();
                    //Compare entered string with database and returns matching results
                    eventRepo.whereGreaterThanOrEqualTo("name", s.toString(), new RepoMultiCallback<Event>() {
                        @Override
                        public void onCallback(ArrayList<Event> models) {
                            for (Event event : models) {
                                //add the the name and user id to the lists send to the recyclerview
                                addEventToExtendedLis(event.getEventId(), searchResultList, mEventSearchListAdapter);
                                    Log.d(TAG, event.getName() + "was found in search");
                            }
                        }
                    });
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void initializeSearchRecyclerView(){
        mEventSearchListAdapter = new EventListAdapter(this, searchResultList, new AdapterCallBack() {
            @Override
            public void onClickCallback(int p) {
                startEventActivity(searchResultList.getId(p));
            }
        });
        //Make the recycler view snap on the current card view
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(mSearchResultRecyclerView);
        // Connect the adapter with the recycler view.
        mSearchResultRecyclerView.setAdapter(mEventSearchListAdapter);
        // Give the recycler view a default layout manager.
        mSearchResultRecyclerView.setLayoutManager(new LinearLayoutManager(ViewEventsActivity.this, RecyclerView.HORIZONTAL, false));
    }

    private void initializeEventsRecyclerView(){
        mEventAttListAdapter = new EventListAdapter(this, eventAttList, new AdapterCallBack() {
            @Override
            public void onClickCallback(int p) {
                startEventActivity(eventAttList.getId(p));
            }
        });
        //Make the recycler view snap on the current card view
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(mEventAttRecyclerView);
        // Connect the adapter with the recycler view.
        mEventAttRecyclerView.setAdapter(mEventAttListAdapter);
        // Give the recycler view a default layout manager.
        mEventAttRecyclerView.setLayoutManager(new LinearLayoutManager(ViewEventsActivity.this, RecyclerView.HORIZONTAL, false));
        updateEventsAttList();
    }

    private void updateEventsAttList() {
        //If there are no friend request then the friend request label and recycler view can be set invisible
        eventRepo.whereArrayContains("eventParticipants", currentUserID, new RepoMultiCallback<Event>() {
            @Override
            public void onCallback(ArrayList<Event> models) {
                for (Event event:models){
                    Log.d(TAG, "Events attending:"+event.getName());
                    addEventToExtendedLis(event.getEventId(), eventAttList, mEventAttListAdapter);
                }
                if (models.size()==0){
                    eventAttLabel.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void initializeEventsInviteRecyclerView(){
        mEventInviteListAdapter = new EventListAdapter(this, eventInviteList, new AdapterCallBack() {
            @Override
            public void onClickCallback(int p) {
                startEventActivity(eventInviteList.getId(p));
            }
        });
        //Make the recycler view snap on the current card view
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(mEventInviteListRecyclerView);
        // Connect the adapter with the recycler view.
        mEventInviteListRecyclerView.setAdapter(mEventInviteListAdapter);
        // Give the recycler view a default layout manager.
        mEventInviteListRecyclerView.setLayoutManager(new LinearLayoutManager(ViewEventsActivity.this, RecyclerView.HORIZONTAL, false));
        updateEventsInviteList();
    }

    private void updateEventsInviteList() {
        eventRepo.whereArrayContains("eventInvited", currentUserID, new RepoMultiCallback<Event>() {
            @Override
            public void onCallback(ArrayList<Event> models) {
                for (Event event:models){
                    Log.d(TAG, "Events invited:"+event.getName());
                    addEventToExtendedLis(event.getEventId(), eventInviteList, mEventInviteListAdapter);
                }
                if (models.size()==0){
                    eventInviteLabel.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void startEventActivity (String eventId){
        final Intent intent = new Intent(this, EventActivity.class);
        intent.putExtra("currentUser", currentUser);
        eventRepo.findById(eventId, new RepoCallback<Event>() {
            @Override
            public void onCallback(Event model) {
                intent.putExtra("currentEvent", model);
                startActivity(intent);
                finish();
            }

            @Override
            public void onGetField(String str) {

            }
        });
    }


    //Helper function to add a storage reference to a list
    public void addStrgRefs(final ExtendedListEvent extList, final RecyclerView.Adapter adapter) {
        for (final String id : extList.getIdList()) {
            final String gsUrl = this.getString(R.string.gsTb256EventPicUrl);
            eventRepo.findById(id, new RepoCallback<Event>() {
                @Override
                public void onCallback(Event model) {
                    storageService.getStorageReference(gsUrl, model.getCoverPicUrl(), new StorageCallback() {
                        @Override
                        public void onSuccess(StorageReference storageReference) {
                            extList.addStrgRef(id, storageReference);
                            adapter.notifyItemChanged(extList.getIdIndexOfLast());
                        }
                        @Override
                        public void onFailure() { }
                    });
                }
                @Override
                public void onGetField(String str) { }
            });
        }
    }


    public void addEventToExtendedLis(String id, final ExtendedListEvent extList, final RecyclerView.Adapter adapter) {
        //Access database to find event corresponding to an id
        eventRepo.findById(id, new RepoCallback<Event>() {
            @Override
            public void onCallback(final Event model) {
                //add the found model to the list

                extList.addElement(model.getName(), model.getEventId(), model.getDate(), model.getCategory(), model.getEventAddress());
                final String gsUrl = getString(R.string.gsTb256EventPicUrl);
                //Add the storage reference to the list
                storageService.getStorageReference(gsUrl, model.getCoverPicUrl(), new StorageCallback() {
                    @Override
                    public void onSuccess(StorageReference storageReference) {
                        extList.addStrgRef(model.getEventId(), storageReference);
                        adapter.notifyItemChanged(extList.getIdIndexOfLast());
                    }

                    @Override
                    public void onFailure() {
                    }
                });
                //Notifies adapter that the list has been updated so recyclerview can be updated
                adapter.notifyItemInserted(extList.getIdIndexOfLast());
            }

            @Override
            public void onGetField(String str) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed Called");
        Intent intent = new Intent(this, HomepageActivity.class);
        intent.putExtra("currentUser", currentUser);
        startActivity(intent);
    }
}
