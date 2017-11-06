package com.chattree.chattree.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.chattree.chattree.home.HomeActivity;
import com.chattree.chattree.network.NetConnectCallback;
import com.chattree.chattree.network.NetworkFragment;
import com.chattree.chattree.tools.JSONMessageParser;
import com.chattree.chattree.tools.Toaster;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;
import static android.support.v4.content.ContextCompat.checkSelfPermission;
import static com.chattree.chattree.login.LoginActivity.EXTRA_LOGIN_DATA;
import static com.chattree.chattree.network.NetworkFragment.HTTP_METHOD_POST;

public class SignupFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, NetConnectCallback<String> {

    /**
     * Keep a reference to the NetworkFragment, which owns the AsyncTask object that is used to execute network ops.
     */
    private NetworkFragment mNetworkFragment = null;

    /**
     * Boolean telling us whether a request is in progress, so we don't trigger overlapping
     * requests with consecutive button clicks.
     */
    private boolean mRequesting = false;

    private static final String SIGNUP_URL_PATH = "signup";

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText             mPasswordView;
    private EditText             mPasswordConfirmView;
    private View                 mProgressView;
    private View                 mSignupFormView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout resource that'll be returned
        View rootView = inflater.inflate(R.layout.fragment_signup, container, false);

        // Get the arguments that was supplied when the fragment was instantiated in the CustomPagerAdapter
//        Bundle args = getArguments();
//        ((TextView) rootView.findViewById(R.id.textView)).setText("Page " + args.getInt("page_position"));

        // Set up the signup form.
        mSignupFormView = rootView.findViewById(R.id.signup_form);
        mProgressView = rootView.findViewById(R.id.signup_progress);

        mEmailView = rootView.findViewById(R.id.email);
        populateAutoComplete();

        mPasswordView = rootView.findViewById(R.id.password);

        mPasswordConfirmView = rootView.findViewById(R.id.password_confirm);
        mPasswordConfirmView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.signup || id == EditorInfo.IME_NULL) {
                    attemptSignup();
                    return true;
                }
                return false;
            }
        });

        Button mSignupButton = rootView.findViewById(R.id.signup_button);
        mSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignup();
            }
        });

        // Create the headless fragment which encapsulates the AsyncTask for the signup op
        mNetworkFragment = NetworkFragment.getInstance(getFragmentManager(), SIGNUP_URL_PATH, HTTP_METHOD_POST);

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
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
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
     * Attempts register the account specified by the signup form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptSignup() {
        if (mRequesting) return;

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mPasswordConfirmView.setError(null);

        // Store values at the time of the signup attempt.
        String email           = mEmailView.getText().toString();
        String password        = mPasswordView.getText().toString();
        String passwordConfirm = mPasswordConfirmView.getText().toString();

        boolean cancel    = false;
        View    focusView = null;

        // Check for a valid password.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.signup_error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid password confirmation.
        if (TextUtils.isEmpty(passwordConfirm)) {
            mPasswordConfirmView.setError(getString(R.string.signup_error_missing_password_confirm));
            focusView = mPasswordConfirmView;
            cancel = true;
        } else if (!isPasswordConfirmValid(password, passwordConfirm)) {
            mPasswordConfirmView.setError(getString(R.string.signup_error_invalid_password_confirm));
            focusView = mPasswordConfirmView;
            cancel = true;
        }

        // Check for a valid email.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.signup_error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt to signup and focus the first form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to perform the user signup attempt.
            showProgress(true);

            if (!mRequesting && mNetworkFragment != null) {
                // Execute the async request
                mRequesting = true;

                try {
                    JSONObject body = new JSONObject();
                    body.put("email", email);
                    body.put("password", password);
                    body.put("confirmPassword", passwordConfirm);

                    mNetworkFragment.startRequest(body.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 3;
    }

    private boolean isPasswordConfirmValid(String password, String confirm) {
        return password.equals(confirm);
    }


    /**
     * Shows the progress UI and hides the signup form.
     */
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mSignupFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mSignupFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS    = 0;
        int IS_PRIMARY = 1;
    }


    private void handleSignupFail(JSONMessageParser jsonParser) throws JSONException {
        String errorType = jsonParser.getData().getString("type");

        if (errorType.equals("email")) {
            mEmailView.setError(getString(R.string.error_incorrect_identifiant));
            mEmailView.requestFocus();
        } else if (errorType.equals("password")) {
            mPasswordView.setError(getString(R.string.error_incorrect_password));
            mPasswordView.requestFocus();
        }
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

    /**
     * Update your UI here based on result of the request.
     *
     * @param result The JSON response
     */
    @Override
    public void updateFromRequest(String result) {
        // Network isn't available
        if (result == null) {
            finishRequesting();
            showProgress(false);
            Toaster.showCustomToast(getActivity(), getString(R.string.error_network_for_signup_toast), Toast.LENGTH_SHORT);
        } else {
            finishRequesting();
            showProgress(false);

            try {
                JSONMessageParser jsonParser = new JSONMessageParser(result);

                // Fail
                if (!jsonParser.getSuccess()) {
                    handleSignupFail(jsonParser);
                }

                // Success -> start the home activity
                else {
                    Intent intent = new Intent(getContext(), HomeActivity.class);
                    intent.putExtra(EXTRA_LOGIN_DATA, jsonParser.getJSONString());
                    startActivity(intent);
                }

            } catch (JSONException e) {
                if (result.equals("HTTP error code: 404")) {
                    finishRequesting();
                    showProgress(false);
                    Toaster.showCustomToast(getActivity(), getString(R.string.error_network_server_down_toast), Toast.LENGTH_SHORT);
                }
                Log.e("JSONException", result);
                e.printStackTrace();
            }

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
            case NetConnectCallback.Progress.ERROR:
                Log.d("SIGNUP", "ERROR, " + progressCode + ", " + percentComplete);
                break;
            case NetConnectCallback.Progress.CONNECT_SUCCESS:
                Log.d("SIGNUP", "CONNECT_SUCCESS, " + progressCode + ", " + percentComplete);
                break;
            case NetConnectCallback.Progress.GET_INPUT_STREAM_SUCCESS:
                Log.d("SIGNUP", "GET_INPUT_STREAM_SUCCESS, " + progressCode + ", " + percentComplete);
                break;
            case NetConnectCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                Log.d("SIGNUP", "PROCESS_INPUT_STREAM_IN_PROGRESS, " + progressCode + ", " + percentComplete);
                break;
            case NetConnectCallback.Progress.PROCESS_INPUT_STREAM_SUCCESS:
                Log.d("SIGNUP", "PROCESS_INPUT_STREAM_SUCCESS, " + progressCode + ", " + percentComplete);
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

}
