package com.mindscribe.models;

import com.google.firebase.Timestamp;

/**
 * User model class — Encapsulation + Constructors
 */
public class User {

    private String userId;
    private String name;
    private String email;
    private String profileImageUrl;
    private Timestamp createdAt;

    // Empty constructor for Firestore
    public User() {}

    // Constructor for creating new user
    public User(String userId, String name, String email) {
        this.userId    = userId;
        this.name      = name;
        this.email     = email;
        this.createdAt = Timestamp.now();
    }

    // Getters & Setters
    public String    getUserId()              { return userId; }
    public void      setUserId(String id)     { this.userId = id; }

    public String    getName()                { return name; }
    public void      setName(String name)     { this.name = name; }

    public String    getEmail()               { return email; }
    public void      setEmail(String email)   { this.email = email; }

    public String    getProfileImageUrl()     { return profileImageUrl; }
    public void      setProfileImageUrl(String url) { this.profileImageUrl = url; }

    public Timestamp getCreatedAt()           { return createdAt; }
    public void      setCreatedAt(Timestamp t){ this.createdAt = t; }

    /** Returns first letter of name for avatar display */
    public String getInitial() {
        if (name == null || name.isEmpty()) return "?";
        return String.valueOf(name.charAt(0)).toUpperCase();
    }
}
