Milton-cloud
============

Milton Cloud (or MC, formerly called Spliffy) is an extensible personal cloud server product. Suitable for
small or large ISP scale installations.

Getting in touch:
 - website: http://cloud.milton.io
 - github: just comment anywhere in here
 - mailing list: http://lists.justthe.net/mailman/listinfo/milton-users
 - contact the author from the website: http://cloud.milton.io/contactus

Features:
 - webdav file server with full version history (internally is just like git, but without explicit commits) 
 - and FTP for the old school types out there
 - file sync (like dropbox) via cross platform client (uses hashsplit4j for efficient delta transfers)
 - contacts, including mobile support via Carddav and desktop support via LDAP
 - calendars, with caldav support
 - application based architecture - extend MC with your own apps!
 - community management. Includes options for public signup, private only and admin approved signups
 - group and role based permission system
 - multiple nested organisations, for delegating authority and teams
 - multiple websites, each re-brandable
 - content management with WYSIWIG editing (thanks to CK editor)
 - video streaming, with automatic generation of multiple formats for iPad+HTML5+flash support
 - analytics and reporting
 - externalisable storage, use Amazon S3, Rackspace cloud, etc for storage.

Downloads
Download from the website here - http://cloud.milton.io/downloads/index.html
But you might find it easier to get started by running the maven commands

Getting started
Easiest way to get started is to build and run from maven, as below. You can also
drop the WAR into tomcat etc, but then you'll just need to do some configuration to
get the initial data setup. If you'd really like to skip maven and go direct to tomcat
let me know.

Prerequisites
 - java 7 JDK (yes, must be version 7!)
 - maven 3

1. clone the MC repo - git@github.com:miltonio/milton-cloud.git
    eg to milton-cloud
2. build the whole MC tree;
    cd milton-cloud
    mvn install
3. Then run the milton-cloud-server project with the "run" profile
    cd milton-cloud-server
    mvn -Prun compile
4. Open your browser to http://localhost:8080 to view the admin console
5. Login with admin/password8
6. Start by creating a website in "Setup websites"
7. Enable whatever apps you want in the website
8. Then view the website (you might need to modify your hosts file to create the virtual host)

Using files, contacts, calendars ... info coming.