package com.guideconnect.config;

import com.guideconnect.model.*;
import com.guideconnect.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Seeds the database with realistic test data on application startup.
 * Skips seeding if data already exists to prevent duplicates.
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final TourListingRepository tourListingRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final TransactionRepository transactionRepository;
    private final MessageRepository messageRepository;
    private final DisputeRepository disputeRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random(42);

    public DataSeeder(UserRepository userRepository,
                      TourListingRepository tourListingRepository,
                      BookingRepository bookingRepository,
                      ReviewRepository reviewRepository,
                      TransactionRepository transactionRepository,
                      MessageRepository messageRepository,
                      DisputeRepository disputeRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tourListingRepository = tourListingRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.transactionRepository = transactionRepository;
        this.messageRepository = messageRepository;
        this.disputeRepository = disputeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping data generation.");
            return;
        }

        log.info("Seeding database with test data...");

        String hashedPassword = passwordEncoder.encode("password123");

        List<User> admins = seedAdminUsers(hashedPassword);
        List<User> tourists = seedTouristUsers(hashedPassword);
        List<User> guides = seedGuideUsers(hashedPassword);
        userRepository.flush();

        List<TourListing> tours = seedTourListings(guides);
        tourListingRepository.flush();

        List<Booking> bookings = seedBookings(tourists, tours);
        bookingRepository.flush();

        seedReviewsAndUpdateRatings(bookings);
        reviewRepository.flush();

        seedTransactions(bookings);
        transactionRepository.flush();

        seedMessages(bookings, tourists, guides);
        messageRepository.flush();

        log.info("Database seeding completed successfully.");
        log.info("  Users: {} admin, {} tourists, {} guides", admins.size(), tourists.size(), guides.size());
        log.info("  Tours: {}", tours.size());
        log.info("  Bookings: {}", bookings.size());
    }

    // ---- Users ----

    private List<User> seedAdminUsers(String hashedPassword) {
        User admin = new User("Admin", "admin@guideconnect.com", hashedPassword, Role.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        return List.of(userRepository.save(admin));
    }

    private List<User> seedTouristUsers(String hashedPassword) {
        String[] firstNames = {
            "Emma", "Liam", "Olivia", "Noah", "Ava", "James", "Sophia", "William", "Isabella", "Oliver",
            "Mia", "Benjamin", "Charlotte", "Elijah", "Amelia", "Lucas", "Harper", "Mason", "Evelyn", "Logan",
            "Abigail", "Alexander", "Emily", "Ethan", "Ella", "Jacob", "Elizabeth", "Michael", "Camila", "Daniel",
            "Luna", "Henry", "Sofia", "Jackson", "Avery", "Sebastian", "Mila", "Aiden", "Aria", "Matthew",
            "Scarlett", "Samuel", "Penelope", "David", "Layla", "Joseph", "Chloe", "Carter", "Victoria", "Owen"
        };
        String[] lastNames = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
            "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
            "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson",
            "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
            "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts"
        };
        String[] langPrefs = {"English", "English, Spanish", "English, French", "English, Japanese", "English, Mandarin"};

        List<User> tourists = new ArrayList<>(50);
        for (int i = 1; i <= 50; i++) {
            User u = new User(
                firstNames[i - 1] + " " + lastNames[i - 1],
                "tourist" + i + "@test.com",
                hashedPassword,
                Role.TOURIST
            );
            u.setStatus(AccountStatus.ACTIVE);
            u.setLanguagePreferences(langPrefs[random.nextInt(langPrefs.length)]);
            u.setContactInfo("+1-555-" + String.format("%04d", 1000 + i));
            u.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(180)));
            tourists.add(u);
        }
        return userRepository.saveAll(tourists);
    }

    private List<User> seedGuideUsers(String hashedPassword) {
        String[] guideNames = {
            "Somchai Wattana", "Yuki Tanaka", "Pierre Dubois", "James Crawford", "Liam O'Brien",
            "Maria Gonzalez", "Ana Santos", "Marco Rossi", "Fatih Yilmaz", "Wayan Sudiarta",
            "Nattaya Srisuk", "Kenji Yamamoto", "Claire Moreau", "Thomas Bennett", "Sophie Laurent",
            "Carlos Mendez", "Lucia Fernandez", "Giovanni Ricci", "Elif Kaya", "Made Surya",
            "Ploy Chaisuwan", "Hiroshi Sato", "Isabelle Leroy", "Edward Blackwell", "Emma Sullivan",
            "Diego Ortega", "Camila Reyes", "Alessandro Conti", "Ayse Demir", "Ketut Darma",
            "Kanya Phongsri", "Takeshi Mori", "Juliette Martin", "George Palmer", "Niamh Walsh",
            "Roberto Herrera", "Valentina Ruiz", "Luca Bianchi", "Zeynep Arslan", "Nyoman Artha",
            "Saranya Kaewsai", "Daichi Kimura", "Amelie Bernard", "Richard Hayes", "Cian Murphy",
            "Fernando Castillo", "Gabriela Torres", "Matteo De Luca", "Ceren Yildiz", "Putu Agung"
        };
        String[][] guideLanguages = {
            {"Thai", "English"}, {"Japanese", "English"}, {"French", "English", "Spanish"},
            {"English"}, {"English", "Irish"}, {"Spanish", "English", "Portuguese"},
            {"Portuguese", "English", "Spanish"}, {"Italian", "English", "French"},
            {"Turkish", "English", "German"}, {"Indonesian", "English"},
            {"Thai", "English", "Mandarin"}, {"Japanese", "English", "Mandarin"},
            {"French", "English"}, {"English", "French"}, {"French", "English", "German"},
            {"Spanish", "English"}, {"Spanish", "English", "French"},
            {"Italian", "English"}, {"Turkish", "English", "Arabic"}, {"Indonesian", "English", "Japanese"},
            {"Thai", "English"}, {"Japanese", "English"}, {"French", "English"},
            {"English"}, {"English"}, {"Spanish", "English"},
            {"Spanish", "English", "Portuguese"}, {"Italian", "English"},
            {"Turkish", "English"}, {"Indonesian", "English"},
            {"Thai", "English", "Japanese"}, {"Japanese", "English", "Korean"},
            {"French", "English", "Italian"}, {"English", "Spanish"}, {"English", "Irish"},
            {"Spanish", "English"}, {"Spanish", "English"}, {"Italian", "English"},
            {"Turkish", "English"}, {"Indonesian", "English"},
            {"Thai", "English"}, {"Japanese", "English"}, {"French", "English"},
            {"English"}, {"English"}, {"Spanish", "English"},
            {"Spanish", "English"}, {"Italian", "English"}, {"Turkish", "English"},
            {"Indonesian", "English"}
        };
        String[] cities = {"Bangkok", "Tokyo", "Paris", "London", "Sydney", "New York", "Barcelona", "Rome", "Bali", "Istanbul"};
        String[] bios = {
            "Born and raised in %s, I have been showing visitors the hidden gems of my city for over %d years.",
            "A certified tour guide with a passion for %s's history and culture. %d years of guiding experience.",
            "Former history teacher turned full-time guide in %s. I love sharing stories that bring the city alive. %d years in tourism.",
            "Professional guide specializing in food and culture tours around %s. %d years of local expertise.",
            "Adventurous spirit based in %s with deep knowledge of local traditions. Guiding for %d years."
        };

        List<User> guides = new ArrayList<>(50);
        for (int i = 1; i <= 50; i++) {
            String city = cities[(i - 1) % cities.length];
            int yearsExp = 3 + random.nextInt(15);
            User u = new User(
                guideNames[i - 1],
                "guide" + i + "@test.com",
                hashedPassword,
                Role.GUIDE
            );
            u.setStatus(AccountStatus.ACTIVE);
            u.setLanguagesSpoken(String.join(", ", guideLanguages[i - 1]));
            u.setBiography(String.format(bios[random.nextInt(bios.length)], city, yearsExp));
            u.setGuidePricing(BigDecimal.valueOf(30 + random.nextInt(121)).setScale(2, RoundingMode.HALF_UP));
            u.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));
            guides.add(u);
        }
        return userRepository.saveAll(guides);
    }

    // ---- Tour Listings ----

    private List<TourListing> seedTourListings(List<User> guides) {
        String[] cities = {"Bangkok", "Tokyo", "Paris", "London", "Sydney", "New York", "Barcelona", "Rome", "Bali", "Istanbul"};
        String[] categories = {"Cultural", "Food & Drink", "Adventure", "Historical", "Nature", "Nightlife", "Photography", "Architecture"};

        String[][] tourTemplates = {
            {"Street Food Discovery Tour", "Explore the vibrant street food scene with a local expert. Taste authentic dishes at hidden stalls and family-run eateries that most tourists never find.", "Central Market Square"},
            {"Ancient Temples and Shrines Walk", "Visit the most sacred and historically significant temples. Learn about centuries of religious traditions and architectural evolution.", "Main Temple Gate"},
            {"Hidden Alleyways and Local Culture", "Wander through narrow alleys and discover the real soul of the city. Meet local artisans, visit community markets, and learn neighborhood stories.", "Old Town Fountain"},
            {"Sunset Photography Tour", "Capture stunning golden-hour shots at the best viewpoints. Suitable for all photography levels, with composition tips and local insights.", "Central Observation Deck"},
            {"Historical Landmarks Walking Tour", "Journey through centuries of history visiting the most important monuments and landmarks. Expert commentary on political, cultural, and social history.", "City Hall Steps"},
            {"Local Craft Beer and Pub Crawl", "Sample the best local brews and visit iconic pubs. Learn about the brewing traditions and the stories behind each venue.", "Historic Pub District"},
            {"Morning Market and Cooking Class", "Start at a bustling morning market selecting fresh ingredients, then head to a local kitchen for a hands-on cooking experience.", "Market East Entrance"},
            {"River and Waterfront Exploration", "Cruise along the river discovering waterfront neighborhoods, historic bridges, and dockside culture.", "Pier 1 Boarding Area"},
            {"Art Galleries and Museum Circuit", "Visit world-class galleries and hidden art spaces. Discuss artistic movements and the local contemporary art scene.", "National Gallery Steps"},
            {"Nightlife and Live Music Tour", "Experience the best of the after-dark scene. Visit jazz clubs, rooftop bars, and live music venues loved by locals.", "Central Nightlife District"},
            {"Countryside Day Trip Adventure", "Escape the city for a day of natural beauty. Visit rural villages, scenic overlooks, and enjoy a farm-to-table lunch.", "Central Bus Terminal"},
            {"Architectural Wonders Tour", "Explore buildings spanning centuries of architectural innovation. From ancient foundations to modern masterpieces.", "Central Square"},
        };

        List<TourListing> tours = new ArrayList<>();
        int tourIndex = 0;

        for (User guide : guides) {
            int toursPerGuide = 2 + random.nextInt(3); // 2-4 tours per guide
            String guideCity = cities[guides.indexOf(guide) % cities.length];

            for (int t = 0; t < toursPerGuide; t++) {
                String[] template = tourTemplates[(tourIndex + t) % tourTemplates.length];
                String city = (t == 0) ? guideCity : cities[random.nextInt(cities.length)];

                TourListing tour = new TourListing();
                tour.setTitle(template[0] + " in " + city);
                tour.setDescription(template[1]);
                tour.setCity(city);
                tour.setMeetingLocation(template[2] + ", " + city);
                tour.setDurationHours(2 + random.nextInt(7)); // 2-8 hours
                tour.setPricePerPerson(BigDecimal.valueOf(20 + random.nextInt(181)).setScale(2, RoundingMode.HALF_UP)); // $20-$200
                tour.setLanguages(guide.getLanguagesSpoken());
                tour.setMaxGroupSize(4 + random.nextInt(9)); // 4-12
                tour.setCategory(categories[random.nextInt(categories.length)]);
                tour.setActive(random.nextDouble() > 0.05); // 95% active
                tour.setGuide(guide);
                tour.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(180)));
                tours.add(tour);
            }
            tourIndex += 3;
        }

        return tourListingRepository.saveAll(tours);
    }

    // ---- Bookings ----

    private List<Booking> seedBookings(List<User> tourists, List<TourListing> tours) {
        BookingStatus[] statuses = BookingStatus.values();
        // Weight distribution: more COMPLETED and CONFIRMED
        BookingStatus[] weightedStatuses = {
            BookingStatus.REQUESTED, BookingStatus.REQUESTED,
            BookingStatus.NEGOTIATING, BookingStatus.NEGOTIATING, BookingStatus.NEGOTIATING,
            BookingStatus.CONFIRMED, BookingStatus.CONFIRMED, BookingStatus.CONFIRMED, BookingStatus.CONFIRMED,
            BookingStatus.COMPLETED, BookingStatus.COMPLETED, BookingStatus.COMPLETED, BookingStatus.COMPLETED,
            BookingStatus.COMPLETED, BookingStatus.COMPLETED, BookingStatus.COMPLETED,
            BookingStatus.CANCELLED, BookingStatus.CANCELLED,
            BookingStatus.REJECTED
        };

        String[] bookingMessages = {
            "Looking forward to this tour! Can we start a bit earlier?",
            "We are a family of four visiting for the first time.",
            "Is this tour suitable for elderly visitors?",
            "Could you include a lunch stop during the tour?",
            "We have a dietary restriction - is that manageable?",
            "Celebrating our anniversary, any special arrangements possible?",
            "First time in the city, very excited!",
            "Can you recommend nearby hotels?",
            "We prefer a slower pace if possible.",
            "Is photography allowed at all the stops?"
        };

        List<Booking> bookings = new ArrayList<>();
        Set<String> usedCombinations = new HashSet<>();

        for (int i = 0; i < 1050; i++) {
            User tourist = tourists.get(random.nextInt(tourists.size()));
            TourListing tour = tours.get(random.nextInt(tours.size()));
            String comboKey = tourist.getId() + "-" + tour.getId() + "-" + (i / 50);

            if (usedCombinations.contains(comboKey)) continue;
            usedCombinations.add(comboKey);

            BookingStatus status = weightedStatuses[random.nextInt(weightedStatuses.length)];

            Booking booking = new Booking();
            booking.setTourist(tourist);
            booking.setTour(tour);
            booking.setGuide(tour.getGuide());
            booking.setRequestedDate(LocalDate.now().plusDays(random.nextInt(60) - 30));
            booking.setRequestedTime(LocalTime.of(8 + random.nextInt(10), random.nextBoolean() ? 0 : 30));
            booking.setGroupSize(1 + random.nextInt(6));
            booking.setStatus(status);
            booking.setMessage(bookingMessages[random.nextInt(bookingMessages.length)]);

            BigDecimal total = tour.getPricePerPerson()
                .multiply(BigDecimal.valueOf(booking.getGroupSize()))
                .multiply(BigDecimal.valueOf(1.10))
                .setScale(2, RoundingMode.HALF_UP);
            booking.setTotalPrice(total);
            booking.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(90)));

            bookings.add(booking);
        }

        return bookingRepository.saveAll(bookings);
    }

    // ---- Reviews ----

    private void seedReviewsAndUpdateRatings(List<Booking> bookings) {
        String[] positiveComments = {
            "Absolutely wonderful experience! Our guide was knowledgeable and friendly.",
            "Best tour I have ever taken. Highly recommend to everyone.",
            "Fantastic local insights. We saw places we never would have found on our own.",
            "Great pace and very informative. The food recommendations were excellent.",
            "An unforgettable day. Our guide made the history come alive.",
            "Perfect for families. The kids loved every minute of it.",
            "Exceeded all expectations. Will definitely book again on our next visit.",
            "Such a genuine and authentic experience. Worth every penny.",
            "Our guide was passionate and made the tour so enjoyable.",
            "Outstanding tour with beautiful sights and great storytelling."
        };
        String[] neutralComments = {
            "Decent tour overall. Some stops were more interesting than others.",
            "Good experience but could have been a bit longer.",
            "The guide was friendly but the pace was a little fast for us.",
            "Nice tour, though the meeting point was a bit hard to find.",
            "Enjoyable afternoon. Would have liked more historical context."
        };
        String[] negativeComments = {
            "Tour was shorter than expected and felt rushed.",
            "The guide seemed distracted and not fully engaged.",
            "Did not match the description. Several stops were skipped.",
            "Average experience. Would not book again at this price."
        };

        List<Booking> completedBookings = bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .collect(Collectors.toList());

        // Map to track ratings per user for average calculation
        Map<Long, List<Integer>> userRatings = new HashMap<>();
        List<Review> reviews = new ArrayList<>();

        for (Booking booking : completedBookings) {
            // Tourist reviews guide (most completed bookings get a review)
            if (random.nextDouble() < 0.85) {
                int rating = generateWeightedRating();
                String comment = getCommentForRating(rating, positiveComments, neutralComments, negativeComments);

                Review review = new Review(
                    booking.getTourist(),
                    booking.getGuide(),
                    booking,
                    rating,
                    comment
                );
                review.setCreatedAt(booking.getCreatedAt().plusDays(1 + random.nextInt(7)));
                reviews.add(review);

                userRatings.computeIfAbsent(booking.getGuide().getId(), k -> new ArrayList<>()).add(rating);
            }

            // Guide reviews tourist (less frequently)
            if (random.nextDouble() < 0.4) {
                int rating = 3 + random.nextInt(3); // Guides tend to rate 3-5
                String comment = "Pleasant group to guide. " + (rating >= 4 ? "Very respectful and engaged." : "On time and cooperative.");

                Review review = new Review(
                    booking.getGuide(),
                    booking.getTourist(),
                    booking,
                    rating,
                    comment
                );
                review.setCreatedAt(booking.getCreatedAt().plusDays(1 + random.nextInt(5)));
                reviews.add(review);

                userRatings.computeIfAbsent(booking.getTourist().getId(), k -> new ArrayList<>()).add(rating);
            }
        }

        reviewRepository.saveAll(reviews);

        // Update average ratings on users
        List<User> usersToUpdate = new ArrayList<>();
        for (Map.Entry<Long, List<Integer>> entry : userRatings.entrySet()) {
            User user = userRepository.findById(entry.getKey()).orElse(null);
            if (user != null) {
                List<Integer> ratings = entry.getValue();
                double avg = ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                user.setAvgRating(Math.round(avg * 100.0) / 100.0);
                user.setReviewCount(ratings.size());
                usersToUpdate.add(user);
            }
        }
        userRepository.saveAll(usersToUpdate);

        log.info("  Reviews: {}", reviews.size());
    }

    private int generateWeightedRating() {
        // Weighted towards higher ratings: 5(35%), 4(30%), 3(20%), 2(10%), 1(5%)
        double roll = random.nextDouble();
        if (roll < 0.05) return 1;
        if (roll < 0.15) return 2;
        if (roll < 0.35) return 3;
        if (roll < 0.65) return 4;
        return 5;
    }

    private String getCommentForRating(int rating, String[] positive, String[] neutral, String[] negative) {
        if (rating >= 4) return positive[random.nextInt(positive.length)];
        if (rating == 3) return neutral[random.nextInt(neutral.length)];
        return negative[random.nextInt(negative.length)];
    }

    // ---- Transactions ----

    private void seedTransactions(List<Booking> bookings) {
        List<Transaction> transactions = new ArrayList<>();

        List<Booking> paidBookings = bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED)
            .collect(Collectors.toList());

        int txnCounter = 1000;
        for (Booking booking : paidBookings) {
            BigDecimal total = booking.getTotalPrice();
            if (total == null) continue;

            BigDecimal commission = total.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            String txnRef = "TXN-" + String.format("%06d", txnCounter++);

            Transaction txn = new Transaction(booking, total, commission, txnRef);
            txn.setPaymentTimestamp(booking.getCreatedAt().plusHours(random.nextInt(24)));
            txn.setStatus(booking.getStatus() == BookingStatus.COMPLETED ? "COMPLETED" : "HELD");
            transactions.add(txn);
        }

        transactionRepository.saveAll(transactions);
        log.info("  Transactions: {}", transactions.size());
    }

    // ---- Messages ----

    private void seedMessages(List<Booking> bookings, List<User> tourists, List<User> guides) {
        String[][] conversationTemplates = {
            {
                "Hi! I am interested in your tour. Is the date flexible?",
                "Hello! Yes, we can adjust the date. What works best for you?",
                "How about next Saturday morning?",
                "Saturday morning works perfectly. I will confirm the booking."
            },
            {
                "Can the tour accommodate wheelchair access?",
                "Absolutely, I can adjust the route to be fully accessible.",
                "That is great to hear. We would also like to add a lunch stop.",
                "No problem, I know a wonderful accessible restaurant along the way."
            },
            {
                "We are a group of six. Is that too many?",
                "Six is fine, my max group size is eight. The more the merrier!",
                "Perfect. Do we need to bring anything special?",
                "Just comfortable walking shoes and a camera!"
            },
            {
                "What is the cancellation policy for this tour?",
                "Free cancellation up to 24 hours before. After that, 50% charge applies.",
                "Understood. We will confirm soon.",
                "Take your time. Let me know if you have any other questions."
            }
        };

        List<Booking> negotiatingBookings = bookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.NEGOTIATING)
            .collect(Collectors.toList());

        List<Message> messages = new ArrayList<>();

        for (Booking booking : negotiatingBookings) {
            String[][] template = {conversationTemplates[random.nextInt(conversationTemplates.length)]};
            int msgCount = 2 + random.nextInt(3); // 2-4 messages per negotiation

            for (int m = 0; m < Math.min(msgCount, template[0].length); m++) {
                boolean touristSends = (m % 2 == 0);
                User sender = touristSends ? booking.getTourist() : booking.getGuide();
                User receiver = touristSends ? booking.getGuide() : booking.getTourist();

                Message msg = new Message(sender, receiver, booking, template[0][m]);
                msg.setTimestamp(booking.getCreatedAt().plusMinutes(30L * (m + 1)));
                messages.add(msg);
            }
        }

        // Add a few flagged messages and disputes
        String[] flaggedContents = {
            "Pay me directly outside the platform to avoid fees.",
            "I can offer you a much cheaper rate if you contact me on WhatsApp.",
            "This guide was rude and unprofessional during the tour.",
            "The tourist was aggressive and made threats.",
            "Give me your personal phone number so we can arrange payment offline."
        };

        List<Booking> disputeBookings = negotiatingBookings.stream()
            .limit(5)
            .collect(Collectors.toList());

        List<Dispute> disputes = new ArrayList<>();

        for (int d = 0; d < disputeBookings.size(); d++) {
            Booking booking = disputeBookings.get(d);
            boolean guideFlags = random.nextBoolean();
            User sender = guideFlags ? booking.getGuide() : booking.getTourist();
            User receiver = guideFlags ? booking.getTourist() : booking.getGuide();
            User reporter = guideFlags ? booking.getTourist() : booking.getGuide();

            Message flaggedMsg = new Message(sender, receiver, booking, flaggedContents[d]);
            flaggedMsg.setFlagged(true);
            flaggedMsg.setTimestamp(booking.getCreatedAt().plusHours(2));
            messages.add(flaggedMsg);

            // Need to save flagged message first to get its ID
            Message savedMsg = messageRepository.save(flaggedMsg);

            Dispute dispute = new Dispute();
            dispute.setBooking(booking);
            dispute.setFlaggedMessage(savedMsg);
            dispute.setReporter(reporter);
            dispute.setDescription("Flagged message reported for policy violation: " + flaggedContents[d]);
            dispute.setStatus(d < 2 ? DisputeStatus.OPEN : (d < 4 ? DisputeStatus.UNDER_REVIEW : DisputeStatus.RESOLVED));
            if (dispute.getStatus() == DisputeStatus.RESOLVED) {
                dispute.setResolution("Reviewed and appropriate action taken. Warning issued to the offending party.");
                dispute.setResolvedAt(LocalDateTime.now().minusDays(random.nextInt(10)));
            }
            dispute.setCreatedAt(booking.getCreatedAt().plusHours(3));
            disputes.add(dispute);
        }

        messageRepository.saveAll(messages);
        disputeRepository.saveAll(disputes);
        log.info("  Messages: {}, Flagged: {}, Disputes: {}", messages.size(), disputeBookings.size(), disputes.size());
    }
}
