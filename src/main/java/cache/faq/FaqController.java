package cache.faq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/faq")
public class FaqController {

    private static final Logger log = LoggerFactory.getLogger(FaqController.class);
    private static final int VECTOR_SIZE = 1536; // text-embedding-3-small

    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;

    public FaqController(EmbeddingService embeddingService, QdrantService qdrantService) {
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
    }

    /**
     * POST /v1/faq/seed
     * Seeds the vector DB with booking system FAQs.
     */
    @PostMapping("/seed")
    public Map<String, Object> seedFaqs() {
        List<Faq> faqs = bookingFaqs();

        // 1. Ensure collection exists
        qdrantService.createCollectionIfNotExists(VECTOR_SIZE);

        // 2. Generate embeddings for all questions in one batch call
        List<String> questions = faqs.stream().map(Faq::getQuestion).toList();
        List<List<Double>> embeddings = embeddingService.embedBatch(questions);

        // 3. Upsert into Qdrant
        qdrantService.upsertFaqs(faqs, embeddings);

        log.info("Seeded {} FAQs into Qdrant", faqs.size());
        return Map.of("status", "seeded", "count", faqs.size());
    }

    /**
     * POST /v1/faq/search
     * Body: { "question": "how do I cancel?" }
     * Returns top 5 matching FAQs with similarity scores.
     */
    @PostMapping("/search")
    public List<FaqSearchResult> searchFaqs(@RequestBody Map<String, String> request) {
        String question = request.get("question");

        // 1. Embed the user's question
        List<Double> queryVector = embeddingService.embed(question);

        // 2. Search Qdrant for top 5
        return qdrantService.search(queryVector, 5);
    }

    private List<Faq> bookingFaqs() {
        return List.of(
            new Faq("How do I create a new booking?",
                    "To create a new booking, log in to your account, select your desired dates and room type, then click 'Book Now'. You'll receive a confirmation email with your booking reference number."),
            new Faq("Can I cancel my booking?",
                    "Yes, you can cancel your booking up to 24 hours before check-in for a full refund. Cancellations within 24 hours may incur a one-night charge. Go to 'My Bookings' and click 'Cancel'."),
            new Faq("How do I modify an existing reservation?",
                    "Navigate to 'My Bookings', select the reservation you want to change, and click 'Modify'. You can update dates, room type, or guest count subject to availability."),
            new Faq("What is the cancellation policy?",
                    "Free cancellation is available up to 24 hours before check-in. Late cancellations or no-shows are charged one night's stay. Special rate bookings may have stricter policies."),
            new Faq("How far in advance can I book?",
                    "You can book up to 365 days in advance. For group bookings of 10 or more rooms, please contact our reservations team directly for availability beyond this window."),
            new Faq("Can I book for someone else?",
                    "Yes, during the booking process you can specify a different guest name. The person making the booking will be the primary contact, but the named guest can check in with valid ID."),
            new Faq("What payment methods are accepted?",
                    "We accept Visa, MasterCard, American Express, PayPal, and bank transfers. Payment is charged at the time of booking unless you select 'Pay at Property'."),
            new Faq("How do I get a refund after cancellation?",
                    "Refunds for eligible cancellations are processed within 5-10 business days to your original payment method. You'll receive an email confirmation once the refund is initiated."),
            new Faq("Will I receive a booking confirmation email?",
                    "Yes, a confirmation email with your booking reference, dates, room details, and check-in instructions is sent immediately after booking. Check your spam folder if you don't see it."),
            new Faq("Can I change the dates of my booking?",
                    "Yes, date changes are subject to availability. Go to 'My Bookings', select your reservation, and click 'Modify Dates'. Price differences will be adjusted automatically."),
            new Faq("What happens if I don't show up for my booking?",
                    "No-shows are charged the full amount of the first night. The remainder of the reservation will be cancelled. Contact us before your check-in time if you're running late."),
            new Faq("How do I view my booking history?",
                    "Log in to your account and go to 'My Bookings'. You'll see all past, current, and upcoming reservations with their details and statuses."),
            new Faq("Can I add extras like breakfast or parking to my booking?",
                    "Yes, you can add extras during booking or later via 'My Bookings'. Available add-ons include breakfast, parking, airport shuttle, late check-out, and spa packages."),
            new Faq("What are the check-in and check-out times?",
                    "Standard check-in is from 3:00 PM and check-out is by 11:00 AM. Early check-in and late check-out can be requested and are subject to availability and additional charges."),
            new Faq("How do I contact customer support for booking issues?",
                    "You can reach our support team via live chat on our website, by emailing support@booking.com, or by calling +1-800-BOOKING. Support is available 24/7."),
            new Faq("Is group booking available?",
                    "Yes, group bookings for 10 or more rooms are available with special rates. Contact our group reservations desk at groups@booking.com or fill out the group booking request form."),
            new Faq("Can I transfer my booking to another person?",
                    "Yes, booking transfers are allowed up to 48 hours before check-in. Contact customer support with the new guest's details and your booking reference to process the transfer."),
            new Faq("What is the minimum and maximum length of stay?",
                    "The minimum stay is 1 night. Maximum stay varies by property but is typically 30 consecutive nights. For extended stays, contact our long-term booking department."),
            new Faq("Do you offer a loyalty or rewards program?",
                    "Yes, our Rewards program lets you earn points on every stay. Points can be redeemed for free nights, upgrades, and partner benefits. Sign up is free at the time of booking."),
            new Faq("How do I apply a promo code or discount?",
                    "Enter your promo code in the 'Promo Code' field on the booking page before confirming. The discount will be reflected in the total price. Promo codes cannot be applied after booking.")
        );
    }
}
