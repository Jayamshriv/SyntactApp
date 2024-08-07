package com.example.syntactapp

import android.Manifest
import android.content.ContentProviderOperation
import android.content.OperationApplicationException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.*
import com.example.syntactapp.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private val TAG = "@@@@"
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "-2-2-2")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.e(TAG, "-1-1-1")
        requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS),
            PackageManager.PERMISSION_GRANTED
        )
        Log.e(TAG, "000")
        binding.addBtn.setOnClickListener {
            Log.e(TAG, "111")
            Log.e(TAG, "222")
            getAndAddContacts()
            Log.e(TAG, "333")
        }

    }

    private fun addContacts(name: String, phoneNumber: String) {
        val contentProviderOps = ArrayList<ContentProviderOperation>()
        contentProviderOps.add(
            ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI
            ).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .build()
        )


        contentProviderOps.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build()
        )

        contentProviderOps.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                )
                .build()
        )

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, contentProviderOps)
        } catch (e: OperationApplicationException) {
            e.printStackTrace()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun getAndAddContacts() {
        Log.e(TAG, "444")

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        Firebase.firestore.collection("collection").get()
            .addOnSuccessListener { result ->
                Log.e(TAG, "555")
                var newContacts = 0
                var existingContacts = 0
                for (document in result) {
                    val name = document.data["name"].toString()
                    val phoneNumber = document.data["phoneNumber"].toString()
                    val dateAdded = document.data["dateAdded"].toString()
                    Log.e(TAG,"$name $phoneNumber $dateAdded")
                    if (dateAdded == today) {
                        if (!isContactExists(phoneNumber)) {
                            newContacts++
                            addContacts(name, phoneNumber)
                            Toast.makeText(this, "Contact added with name $name", Toast.LENGTH_SHORT).show()
                        } else {
                            existingContacts++
                        }
                    }
                    binding.contactsAddedTv.text =
                        "$newContacts New Contacts added with date $dateAdded\n $existingContacts exists already in contacts"
                    Log.e(TAG, "${document.id} => ${document.data}")
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "666")
                Log.e(TAG, exception.toString())
            }
    }


    fun isContactExists(number: String): Boolean {
        // number is the phone number
        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        );
        val phoneNumberProjection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.NUMBER,
            ContactsContract.PhoneLookup.DISPLAY_NAME
        )
        val cur = contentResolver.query(lookupUri, phoneNumberProjection, null, null, null)!!
        try {
            if (cur.moveToFirst()) {
                cur.close();
                return true;
            }
        } finally {
            if (cur != null)
                cur.close();
        }
        return false;
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PackageManager.PERMISSION_GRANTED) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.e(TAG, "Permissions granted")
            } else {
                Log.e(TAG, "Permissions denied")
            }
        }
    }

}