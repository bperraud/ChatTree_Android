package com.chattree.chattree.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;


import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.chattree.chattree.R;
import network.NetConnectCallback;
import network.NetworkFragment;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;
import static android.support.v4.content.ContextCompat.checkSelfPermission;

public class LoginFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, NetConnectCallback<String> {

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };

    /**
     * Keep a reference to the NetworkFragment, which owns the AsyncTask object that is used to execute network ops.
     */
    private NetworkFragment mNetworkFragment = null;

    /**
     * Boolean telling us whether a download is in progress, so we don't trigger overlapping
     * requests with consecutive button clicks.
     */
    private boolean mRequesting = false;

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    // UI references.
    private AutoCompleteTextView mIdentifiantView;
    private EditText             mPasswordView;
    private View                 mProgressView;
    private View                 mLoginFormView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);

        // Get the arguments that was supplied when the fragment was instantiated in the CustomPagerAdapter
//        Bundle args = getArguments();
//        ((TextView) rootView.findViewById(R.id.textView)).setText("Page " + args.getInt("page_position"));

        // Set up the login form.
        mLoginFormView = rootView.findViewById(R.id.login_form);
        mProgressView = rootView.findViewById(R.id.login_progress);

        mIdentifiantView = rootView.findViewById(R.id.identifiant);
        populateAutoComplete();

        mPasswordView = rootView.findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mLoginButton = rootView.findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        // Create the headless fragment which encapsulates the AsyncTask for the login op
        mNetworkFragment = NetworkFragment.getInstance(getFragmentManager(), "https://www.google.com");

        return rootView;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(getContext(), READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mIdentifiantView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{ READ_CONTACTS }, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{ READ_CONTACTS }, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mRequesting) return;

        // Force to hide the keyboard
//        InputMethodManager inputManager =
//                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//        inputManager.hideSoftInputFromWindow(
//                getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS
//        );

        // Reset errors.
        mIdentifiantView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String identifiant = mIdentifiantView.getText().toString();
        String password    = mPasswordView.getText().toString();

        boolean cancel    = false;
        View    focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid identifiant.
        if (TextUtils.isEmpty(identifiant)) {
            mIdentifiantView.setError(getString(R.string.error_field_required));
            focusView = mIdentifiantView;
            cancel = true;
        } else if (!isIdentifiantValid(identifiant)) {
            mIdentifiantView.setError(getString(R.string.error_invalid_identifiant));
            focusView = mIdentifiantView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to perform the user login attempt.
            showProgress(true);

            if (!mRequesting && mNetworkFragment != null) {
                // Execute the async request
                mRequesting = true;
                mNetworkFragment.startRequest();
            }
        }
    }

    private boolean isIdentifiantValid(String identifiant) {
        return identifiant.length() > 2;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, emailAddressCollection);
        mIdentifiantView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS    = 0;
        int IS_PRIMARY = 1;
    }

    // --------------------------------------------------------------------------------------
    // ---------------- LoaderManager.LoaderCallbacks<Cursor> Implementation ----------------
    // --------------------------------------------------------------------------------------

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getContext(),
                                // Retrieve data rows for the device user's 'profile' contact.
                                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                                                     ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                                // Select only email addresses.
                                ContactsContract.Contacts.Data.MIMETYPE +
                                " = ?", new String[]{ ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE },

                                // Show primary email addresses first. Note that there won't be
                                // a primary email address if the user hasn't specified one.
                                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }


    // -------------------------------------------------------------------------------------------
    // ---------------------------- NetConnectCallback Implementation ----------------------------
    // -------------------------------------------------------------------------------------------

    @Override
    public void updateFromRequest(String result) {
        // Update your UI here based on result of the request.

        // No network
        if (result == null) {
            Toast.makeText(getContext(), getString(R.string.error_network_for_login_toast), Toast.LENGTH_SHORT).show();
            finishRequesting();
            showProgress(false);
        }

        else {
//            Toast.makeText(getContext(), "RESULT : " + result, Toast.LENGTH_SHORT).show();
            Log.d("LOGIN", "RESULT : " + result);

            finishRequesting();
            showProgress(false);
        }

    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        switch (progressCode) {
            // You can add UI behavior for progress updates here.
            case Progress.ERROR:
                Log.d("LOGIN", "ERROR, " + progressCode + ", " + percentComplete);
                break;
            case Progress.CONNECT_SUCCESS:
                Log.d("LOGIN", "CONNECT_SUCCESS, " + progressCode + ", " + percentComplete);
                break;
            case Progress.GET_INPUT_STREAM_SUCCESS:
                Log.d("LOGIN", "GET_INPUT_STREAM_SUCCESS, " + progressCode + ", " + percentComplete);
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                Log.d("LOGIN", "PROCESS_INPUT_STREAM_IN_PROGRESS, " + progressCode + ", " + percentComplete);
                break;
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                Log.d("LOGIN", "PROCESS_INPUT_STREAM_SUCCESS, " + progressCode + ", " + percentComplete);
                break;
        }
    }

    @Override
    public void finishRequesting() {
        mRequesting = false;
        if (mNetworkFragment != null) {
            mNetworkFragment.cancelRequest();
        }
    }










    // -------------------------------------------------------------------------
    // --------------------------------- TASKS ---------------------------------
    // -------------------------------------------------------------------------


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mIdentifiant;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mIdentifiant = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mIdentifiant)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
//            mAuthTask = null;
            showProgress(false);

            if (success) {
                getActivity().finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
//            mAuthTask = null;
            showProgress(false);
        }
    }

}
