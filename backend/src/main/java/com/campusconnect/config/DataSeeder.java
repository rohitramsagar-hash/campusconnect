package com.campusconnect.config;

import com.campusconnect.model.*;
import com.campusconnect.repository.*;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Fills an empty database with demo users, categories and complaints (runs only when there are no users). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final CategoryRepository categories;
    private final ComplaintRepository complaints;
    private final StatusHistoryRepository history;
    private final CommentRepository comments;
    private final UpvoteRepository upvotes;
    private final PasswordEncoder encoder;
    private final Instant now = Instant.now();

    public DataSeeder(UserRepository users, CategoryRepository categories, ComplaintRepository complaints,
                      StatusHistoryRepository history, CommentRepository comments, UpvoteRepository upvotes,
                      PasswordEncoder encoder) {
        this.users = users;
        this.categories = categories;
        this.complaints = complaints;
        this.history = history;
        this.comments = comments;
        this.upvotes = upvotes;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        User admin = user("Campus Admin", "admin@campus.edu", "Admin@123", Role.ADMIN, "Administration");
        User ravi = user("Ravi Kumar", "ravi.staff@campus.edu", "Staff@123", Role.STAFF, "Maintenance");
        User priya = user("Priya Sharma", "priya.staff@campus.edu", "Staff@123", Role.STAFF, "IT Services");
        User ananya = user("Ananya Reddy", "student@campus.edu", "Student@123", Role.STUDENT, "CSE");
        User rahul = user("Rahul Verma", "rahul@campus.edu", "Student@123", Role.STUDENT, "ECE");

        Category hostel = category("Hostel", "Rooms, furniture, hostel facilities");
        Category classrooms = category("Classrooms & Labs", "Projectors, benches, lab equipment");
        Category it = category("IT & Wi-Fi", "Internet, campus portal, computer labs");
        Category library = category("Library", "Books, seating, timings");
        Category transport = category("Transport", "College buses and parking");
        Category canteen = category("Canteen & Food", "Canteen and mess food quality, hygiene");
        Category sanitation = category("Sanitation & Water", "Washrooms, drinking water, cleanliness");
        category("Electrical", "Fans, lights, power cuts");
        Category academics = category("Academics", "Timetable, exams, certificates, scholarships");
        Category safety = category("Safety & Security", "Lighting, CCTV, security staff");
        category("Other", "Anything else");

        Complaint wifi = complaint("Wi-Fi keeps disconnecting in Block C, 3rd floor",
                "The Wi-Fi drops every 10-15 minutes in rooms C-301 to C-310. Online lab submissions keep failing.",
                "Block C, 3rd floor", it, Priority.HIGH, ananya, 96, false, true);
        assign(wifi, priya, admin, 2);
        move(wifi, ComplaintStatus.IN_PROGRESS, priya, 6, "Faulty access point found near C-305. Replacement ordered.");
        comment(wifi, priya, "We've isolated the issue to one access point. New hardware arrives tomorrow.", 7);
        comment(wifi, ananya, "Thank you! Is there a temporary workaround till then?", 9);

        Complaint cooler = complaint("Water cooler leaking near the central library",
                "The water cooler outside the library entrance is leaking and the floor stays wet. Someone may slip.",
                "Central Library entrance", sanitation, Priority.MEDIUM, rahul, 40, false, true);

        Complaint projector = complaint("Projector in Room 204 flickering during lectures",
                "The projector screen flickers every few minutes, making slides hard to read.",
                "Academic Block, Room 204", classrooms, Priority.LOW, ananya, 240, false, true);
        assign(projector, ravi, admin, 3);
        move(projector, ComplaintStatus.IN_PROGRESS, ravi, 20, null);
        move(projector, ComplaintStatus.RESOLVED, ravi, 30, "Replaced the HDMI cable and projector lamp.");
        move(projector, ComplaintStatus.CLOSED, ananya, 40, "Working fine now, thanks!");

        Complaint mess = complaint("Mess food served cold at dinner",
                "For the past week dinner in the boys' mess has been served cold after 8 PM.",
                "Boys' hostel mess", canteen, Priority.MEDIUM, rahul, 144, true, true);
        assign(mess, ravi, admin, 4);
        move(mess, ComplaintStatus.IN_PROGRESS, ravi, 10, null);
        move(mess, ComplaintStatus.RESOLVED, ravi, 50, "Food warmers installed at the serving counter.");

        Complaint bus = complaint("Bus route 5 arrives 20 minutes late every morning",
                "Route 5 (Dilsukhnagar) reaches campus at 9:20 while classes start at 9:00.",
                "Main gate bus bay", transport, Priority.HIGH, ananya, 120, false, true);

        Complaint light = complaint("Streetlight not working near the girls' hostel gate",
                "The streetlight at the hostel gate has been off for two nights. The path is completely dark.",
                "Girls' hostel gate", safety, Priority.URGENT, rahul, 20, false, true);
        assign(light, ravi, admin, 1);

        Complaint libraryHours = complaint("Extend library hours during exam weeks",
                "Please keep the library open till 11 PM during semester exams.",
                "Central Library", library, Priority.LOW, ananya, 288, false, true);
        move(libraryHours, ComplaintStatus.REJECTED, admin, 24,
                "Library timings are set by the university. Extended reading-room hours will be announced for exam week.");

        Complaint scholarship = complaint("Scholarship form shows 'server error' on submit",
                "The scholarship portal shows a server error when I upload my income certificate.",
                null, academics, Priority.MEDIUM, rahul, 72, false, false);
        assign(scholarship, priya, admin, 5);

        complaints.flush();
        upvote(cooler, ananya);
        upvote(bus, rahul);
        upvote(light, ananya);
        upvote(wifi, rahul);

        log.info("Demo data created. Logins: admin@campus.edu / Admin@123, ravi.staff@campus.edu / Staff@123, "
                + "student@campus.edu / Student@123");
    }

    private User user(String name, String email, String password, Role role, String dept) {
        return users.save(new User(name, email, encoder.encode(password), role, dept));
    }

    private Category category(String name, String description) {
        return categories.save(new Category(name, description));
    }

    private Complaint complaint(String title, String description, String location, Category category,
                                Priority priority, User reporter, long hoursAgo, boolean anonymous, boolean isPublic) {
        Complaint c = new Complaint();
        c.setTitle(title);
        c.setDescription(description);
        c.setLocation(location);
        c.setCategory(category);
        c.setPriority(priority);
        c.setCreatedBy(reporter);
        c.setAnonymous(anonymous);
        c.setPublicVisible(isPublic);
        c.setCreatedAt(now.minus(Duration.ofHours(hoursAgo)));
        complaints.save(c);
        StatusHistory h = new StatusHistory(c, null, ComplaintStatus.OPEN, reporter, "Complaint submitted");
        h.setCreatedAt(c.getCreatedAt());
        history.save(h);
        return c;
    }

    private void assign(Complaint c, User staff, User admin, long hoursAfter) {
        c.setAssignedTo(staff);
        StatusHistory h = new StatusHistory(c, c.getStatus(), c.getStatus(), admin, "Assigned to " + staff.getName());
        h.setCreatedAt(c.getCreatedAt().plus(Duration.ofHours(hoursAfter)));
        history.save(h);
    }

    private void move(Complaint c, ComplaintStatus to, User by, long hoursAfter, String note) {
        Instant at = c.getCreatedAt().plus(Duration.ofHours(hoursAfter));
        StatusHistory h = new StatusHistory(c, c.getStatus(), to, by, note);
        h.setCreatedAt(at);
        history.save(h);
        c.setStatus(to);
        if (to == ComplaintStatus.RESOLVED) {
            c.setResolvedAt(at);
        }
    }

    private void comment(Complaint c, User author, String message, long hoursAfter) {
        Comment comment = new Comment(c, author, message);
        comment.setCreatedAt(c.getCreatedAt().plus(Duration.ofHours(hoursAfter)));
        comments.save(comment);
    }

    private void upvote(Complaint c, User user) {
        upvotes.save(new Upvote(c, user));
        complaints.changeUpvoteCount(c.getId(), 1);
    }
}
