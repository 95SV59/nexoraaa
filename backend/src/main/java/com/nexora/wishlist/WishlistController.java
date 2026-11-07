package com.nexora.wishlist;

import com.nexora.security.AuthPrincipal;
import com.nexora.wishlist.dto.WishlistItemDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public List<WishlistItemDto> getMyWishlist(@AuthenticationPrincipal AuthPrincipal principal) {
        return wishlistService.getUserWishlist(principal.userId());
    }

    @PostMapping("/{slug}")
    public ResponseEntity<WishlistItemDto> addToWishlist(@PathVariable String slug,
                                                         @AuthenticationPrincipal AuthPrincipal principal) {
        WishlistItemDto item = wishlistService.addToWishlist(principal.userId(), slug);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @DeleteMapping("/{slug}")
    public Map<String, Object> removeFromWishlist(@PathVariable String slug,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        wishlistService.removeFromWishlist(principal.userId(), slug);
        return Map.of("removed", true, "slug", slug);
    }

    @GetMapping("/{slug}/check")
    public Map<String, Object> checkWishlist(@PathVariable String slug,
                                             @AuthenticationPrincipal AuthPrincipal principal) {
        boolean inWishlist = wishlistService.isInWishlist(principal.userId(), slug);
        return Map.of("inWishlist", inWishlist, "slug", slug);
    }
}
