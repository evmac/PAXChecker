package com.github.sunnybat.paxchecker;

import com.github.sunnybat.commoncode.email.EmailAccount;
import com.github.sunnybat.commoncode.error.ErrorBuilder;
import com.github.sunnybat.commoncode.startup.LoadingWindow;
import com.github.sunnybat.commoncode.update.*;
import com.github.sunnybat.paxchecker.browser.Browser;
import com.github.sunnybat.paxchecker.check.*;
import com.github.sunnybat.paxchecker.gui.Status;
import com.github.sunnybat.paxchecker.gui.Tickets;
import com.github.sunnybat.paxchecker.notification.NotificationHandler;
import com.github.sunnybat.paxchecker.setup.*;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

/**
 *
 * @author SunnyBat
 */
public final class PAXChecker {

  public static final String VERSION = "3.0.0 R2";
  private static final Object CLINE_LOCK = new Object();
  private static boolean commandLine;
  private static final String PATCH_NOTES_LINK = "https://dl.orangedox.com/r29siEtUhPNW4FKg7T/PAXCheckerUpdates.txt?dl=1";
  private static final String UPDATE_LINK = "https://dl.orangedox.com/TXu5eUDa2Ds3RSKVUI/PAXChecker.jar?dl=1";
  private static final String BETA_UPDATE_LINK = "https://dl.orangedox.com/BqkMXYrpYjlBEbfVmd/PAXCheckerBETA.jar?dl=1";

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    removeOldPreferences();
    System.out.println("Initializing...");
    java.net.HttpURLConnection.setFollowRedirects(true); // Follow all redirects automatically, so when checking Showclix event pages, it works!

//    ResourceDownloader download = new ResourceDownloader();
//    download.downloadResources();
    // SETUP
    boolean isHeadless = GraphicsEnvironment.isHeadless();
    boolean isAutostart = false;
    boolean checkUpdates = true;
    boolean checkNotifications = true;
    boolean startMinimized = false;
    List<String> followList = new ArrayList<>();
    Map<String, String> properties = new HashMap<>();
    String[] twitterTokens = new String[4];
    for (int i = 0; i < args.length; i++) {
      switch (args[i].toLowerCase()) {
        case "-cli":
          isHeadless = true;
          break;
        case "-autostart":
          isAutostart = true;
          break;
        case "-noupdate":
          checkUpdates = false;
          break;
//          case "-notificationid":
//            NotificationHandler.setLastNotificationID(args[a + 1]);
//            break;
        case "-nonotifications":
          checkNotifications = false;
          break;
        case "-follow":
        case "-checktwitter":
          followList.add(args[i + 1]);
          break;
        case "-startminimized":
          startMinimized = true;
          break;
        case "-property":
          if (args.length > i + 2) {
            String key = args[i + 1];
            String value = args[i + 2];
            if (!key.startsWith("-") && !value.startsWith("-")) {
              properties.put(key, value);
            } else {
              System.out.println("Not enough arguments specified for -property!");
            }
          } else {
            System.out.println("Not enough arguments specified for -property!");
          }
          break;
        case "-twitterkeys":
          twitterTokens[0] = args[i + 1];
          twitterTokens[1] = args[i + 2];
          twitterTokens[2] = args[i + 3];
          twitterTokens[3] = args[i + 4];
          break;
        case "-savelog":
          try {
            SavePrintStream printer;
            if (args.length > i + 1 && !args[i + 1].startsWith("-")) {
              printer = new SavePrintStream(System.out, args[i + 1]);
            } else {
              printer = new SavePrintStream(System.out);
            }
            System.setOut(printer); // TODO: Separate these so they print to the same file but print to out/err properly
            System.setErr(printer);
          } catch (FileNotFoundException fnfe) {
            System.out.println("Unable to set output stream saver -- File does not exist?");
            fnfe.printStackTrace();
          }
          break;
        case "-alarmfile":
          Audio.setAlarmFile(args[i + 1]);
          break;
        default:
          if (args[i].startsWith("-")) {
            System.out.println("Unknown argument: " + args[i]);
          }
          break;
      }
    }
    if (isHeadless) {
      ErrorBuilder.forceCommandLine();
    }
    String patchNotes = doLoading(checkUpdates, checkNotifications);
    Setup mySetup;
    if (isAutostart) {
      mySetup = new SetupAuto(args);
    } else if (isHeadless) {
      mySetup = new SetupCLI();
    } else {
      SetupGUI myGUI = new SetupGUI(twitterTokens);
      if (patchNotes != null) {
        myGUI.setPatchNotesText(patchNotes);
      } else {
        myGUI.setPatchNotesText("[Updating Disabled]");
      }
      mySetup = myGUI;
    }
    mySetup.promptForSettings(); // Might take a while
    twitterTokens[0] = mySetup.getTwitterConsumerKey();
    twitterTokens[1] = mySetup.getTwitterConsumerSecret();
    twitterTokens[2] = mySetup.getTwitterApplicationKey();
    twitterTokens[3] = mySetup.getTwitterApplicationSecret();

    // SET UP STATUS WINDOW AND CHECKERS
    Browser myBrowser = new Browser();
    myBrowser.setExpo(mySetup.getExpoToCheck());
    Status myStatus;
    EmailAccount emailAccount;
    try {
      emailAccount = new EmailAccount(mySetup.getEmailUsername(), mySetup.getEmailPassword());
      for (String s : mySetup.getEmailAddresses()) {
        emailAccount.addEmailAddress(s);
      }
      for (String key : properties.keySet()) {
        emailAccount.setProperty(key, properties.get(key));
      }
      myStatus = new Status(myBrowser.getExpo(), emailAccount.getUsername(), emailAccount.getAddressList());
    } catch (IllegalArgumentException e) {
      emailAccount = null;
      myStatus = new Status(myBrowser.getExpo());
    }
    myStatus.enableEmail();
    if (mySetup.shouldPlayAlarm()) {
      Audio.enableAlarm();
      myStatus.enableAlarm();
    }
    final TicketChecker myChecker = initChecker(mySetup, myStatus, myBrowser.getExpo());
    if (mySetup.shouldCheckTwitter()) {
      TwitterStreamer tcheck = setupTwitter(myStatus, twitterTokens);
      for (String s : followList) {
        tcheck.addUser(s);
      }
      tcheck.startStreamingTwitter(); // CHECK: Move this down?
      myStatus.enableTwitter();
    }
    if (startMinimized) {
      myStatus.minimizeWindow();
    } else {
      myStatus.showWindow();
    }

    // START CHECKING
    checkForTickets(myStatus, myChecker, emailAccount, mySetup.timeBetweenChecks());
  }

  private static boolean hasArgument(String[] args, String argument) {
    for (String s : args) {
      if (s.toLowerCase().equals(argument.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes the old Preferences root to prevent registry cluttering.
   */
  private static void removeOldPreferences() {
    try {
      System.out.println("Checking for old preferences root...");
      java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("paxchecker");
      p.removeNode();
      p.flush();
      System.out.println("Removed node.");
    } catch (Exception e) {
      System.out.println("Unable to remove old preferences node -- already removed?");
    }
  }

  /**
   * Loads Updates and Notifications.
   *
   * @return All the Version Notes, or null if not loaded
   */
  private static String doLoading(boolean checkUpdates, boolean checkNotifications) {
    LoadingWindow loadingWindow = new LoadingWindow();
    loadingWindow.showWindow();
    String notes = null;

    // CHECK UPDATES
    if (checkUpdates) {
      try {
        loadingWindow.setStatus("Checking for updates...");
        PatchNotesDownloader notesDownloader = new PatchNotesDownloader(PATCH_NOTES_LINK);
        notesDownloader.downloadVersionNotes(VERSION);
        notes = notesDownloader.getVersionNotes();
        if (notesDownloader.updateAvailable()) {
          // TODO: Add support for anonymous downloads
          UpdateDownloader myDownloader = new UpdateDownloader(UPDATE_LINK, BETA_UPDATE_LINK);
          UpdatePrompt myPrompt = new UpdatePrompt("PAXChecker", myDownloader.getUpdateSize(), notesDownloader.getUpdateLevel(),
              "VERSION", notesDownloader.getVersionNotes(VERSION));
          loadingWindow.setVisible(false); // TODO: Make better method
          myPrompt.showWindow();
          try {
            myPrompt.waitForClose();
          } catch (InterruptedException e) {
          }
          if (myPrompt.shouldUpdateProgram()) {
            myDownloader.updateProgram(myPrompt, new File(PAXChecker.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
            System.exit(0); // TODO: Is this actually the right way to kill the program?
          } else {
            loadingWindow.showWindow();
          }
        }
      } catch (IOException | URISyntaxException e) {
        e.printStackTrace();
      }
    }

    if (checkNotifications) {
      loadingWindow.setStatus("Checking for notifications...");
      NotificationHandler notifications = new NotificationHandler(false, "-1"); // TODO: Load notifications from Preferences
      notifications.loadNotifications();
      notifications.showNewNotifications();
    }
    loadingWindow.dispose();
    return notes;
  }

  private static TicketChecker initChecker(Setup mySetup, Status myStatus, String expo) {
    TicketChecker myChecker = new TicketChecker(myStatus);
    if (mySetup.shouldCheckPAXWebsite()) {
      myChecker.addChecker(new CheckPaxsite(expo));
    }
    if (mySetup.shouldCheckShowclix()) {
      myChecker.addChecker(new CheckShowclix(expo));
    }
    if (mySetup.shouldCheckKnownEvents()) {
      myChecker.addChecker(new CheckShowclixEventPage());
    }
    myChecker.initCheckers();
    return myChecker;
  }

  private static TwitterStreamer setupTwitter(final Status myStatus, final String[] keys) {
    return new TwitterStreamer(keys) {
      @Override
      public void twitterConnected() {
        myStatus.setTwitterStatus(true);
      }

      @Override
      public void twitterDisconnected() {
        myStatus.setTwitterStatus(false);
      }

      @Override
      public void twitterKilled() {
        myStatus.twitterStreamKilled();
      }

      @Override
      public void linkFound(String link) {
        Browser.openLinkInBrowser(link);
      }
    };
  }

  /**
   * Checks for tickets. Blocks indefinitely. Note that if checker is not checking anything, this method does not block and only modifies the Status
   * GUI status text.
   *
   * @param status The Status window to update
   * @param checker The TicketChecker to use
   * @param email The EmailAccount to use, or null for none
   * @param checkTime The time between checks
   * @throws NullPointerException if any arguments besides email are null
   */
  private static void checkForTickets(Status status, TicketChecker checker, EmailAccount email, int checkTime) {
    if (checker.isCheckingAnything()) {
      while (true) {
        status.setLastCheckedText("Checking for Updates");
        long startTime = System.currentTimeMillis();
        if (checker.isUpdated()) {
          Browser.openLinkInBrowser(checker.getLinkFound());
          if (email != null) {
            email.sendMessage("PAXChecker", "A new link has been found: " + checker.getLinkFound());
          }
          Tickets ticketWindow = new Tickets(checker.getLinkFound()); // CHECK: Should I only allow one Tickets at a time?
          ticketWindow.showWindow();
        }
        status.setDataUsageText(DataTracker.getDataUsedMB());
        while (System.currentTimeMillis() - startTime < checkTime * 1000) {
          status.setLastCheckedText(checkTime - (int) ((System.currentTimeMillis() - startTime) / 1000));
          try {
            Thread.sleep(250);
          } catch (InterruptedException iE) {
          }
        }
      }
    } else {
      status.setLastCheckedText("[Only Checking Twitter]");
    }
  }

  public static void enableCommandLine() { // TODO: Remove this
    synchronized (CLINE_LOCK) {
      commandLine = true;
    }
  }

  public static boolean isCommandLine() {
    synchronized (CLINE_LOCK) {
      return commandLine;
    }
  }

  /**
   * Creates a new non-daemon Thread with the given Runnable object.
   *
   * @param run The Runnable object to use
   */
  public static void continueProgram(Runnable run) { // CHECK: Remove?
    Thread newThread = new Thread(run);
    newThread.setName("Program Loop");
    newThread.setDaemon(false); // Prevent the JVM from stopping due to zero non-daemon threads running
    newThread.setPriority(Thread.NORM_PRIORITY);
    newThread.start(); // Start the Thread
  }

  /**
   * Starts a new daemon Thread.
   *
   * @param run The Runnable object to use
   * @param name The name to give the Thread
   */
  public static void startBackgroundThread(Runnable run, String name) {
    Thread newThread = new Thread(run);
    newThread.setName(name);
    newThread.setDaemon(true); // Kill the JVM if only daemon threads are running
    newThread.setPriority(Thread.MIN_PRIORITY); // Let other Threads take priority, as this will probably not run for long
    newThread.start(); // Start the Thread
  }
}
