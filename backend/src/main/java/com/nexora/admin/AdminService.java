package com.nexora.admin;

import com.nexora.admin.dto.AdminStatsDto;
import com.nexora.admin.dto.CreateGameRequest;
import com.nexora.admin.dto.UserManagementDto;
import com.nexora.article.ArticleRepository;
import com.nexora.exception.ConflictException;
import com.nexora.exception.ResourceNotFoundException;
import com.nexora.game.*;
import com.nexora.game.dto.GameDetailDto;
import com.nexora.multiplayer.room.GameRoomRepository;
import com.nexora.multiplayer.room.RoomStatus;
import com.nexora.multiplayer.session.GameSessionRepository;
import com.nexora.review.ReviewRepository;
import com.nexora.user.Role;
import com.nexora.user.RoleRepository;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GameRepository gameRepository;
    private final GenreRepository genreRepository;
    private final PlatformRepository platformRepository;
    private final GameMediaRepository mediaRepository;
    private final ReviewRepository reviewRepository;
    private final ArticleRepository articleRepository;
    private final GameSessionRepository sessionRepository;
    private final GameRoomRepository roomRepository;

    public AdminService(UserRepository userRepository, RoleRepository roleRepository,
                        GameRepository gameRepository, GenreRepository genreRepository,
                        PlatformRepository platformRepository, GameMediaRepository mediaRepository,
                        ReviewRepository reviewRepository, ArticleRepository articleRepository,
                        GameSessionRepository sessionRepository, GameRoomRepository roomRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.gameRepository = gameRepository;
        this.genreRepository = genreRepository;
        this.platformRepository = platformRepository;
        this.mediaRepository = mediaRepository;
        this.reviewRepository = reviewRepository;
        this.articleRepository = articleRepository;
        this.sessionRepository = sessionRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == User.Status.ACTIVE).count();
        long totalGames = gameRepository.count();
        long reported = reviewRepository.findByReportedTrueOrderByCreatedAtDesc().size();
        long totalArticles = articleRepository.count();
        long totalSessions = sessionRepository.count();
        long activeRooms = roomRepository.countByStatus(RoomStatus.LOBBY) + roomRepository.countByStatus(RoomStatus.IN_PROGRESS);

        return new AdminStatsDto(totalUsers, activeUsers, totalGames, reported, totalArticles, totalSessions, activeRooms);
    }

    @Transactional(readOnly = true)
    public List<UserManagementDto> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserManagementDto(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getRole() != null ? u.getRole().getName() : Role.PLAYER,
                        u.getStatus().name(),
                        u.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserManagementDto toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setStatus(user.getStatus() == User.Status.ACTIVE ? User.Status.SUSPENDED : User.Status.ACTIVE);
        User saved = userRepository.save(user);
        return new UserManagementDto(saved.getId(), saved.getUsername(), saved.getEmail(),
                saved.getRole().getName(), saved.getStatus().name(), saved.getCreatedAt());
    }

    @Transactional
    public UserManagementDto changeUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
        user.setRole(role);
        User saved = userRepository.save(user);
        return new UserManagementDto(saved.getId(), saved.getUsername(), saved.getEmail(),
                saved.getRole().getName(), saved.getStatus().name(), saved.getCreatedAt());
    }

    @Transactional
    public GameDetailDto createGame(CreateGameRequest req) {
        if (gameRepository.findBySlug(req.getSlug()).isPresent()) {
            throw new ConflictException("Game slug already exists: " + req.getSlug());
        }

        Genre genre = null;
        if (req.getGenreName() != null && !req.getGenreName().isBlank()) {
            genre = genreRepository.findByName(req.getGenreName())
                    .orElseGet(() -> genreRepository.save(new Genre(req.getGenreName())));
        }

        Game game = new Game(req.getSlug(), req.getTitle(), req.getDescription(),
                req.getDeveloper() != null ? req.getDeveloper() : "NEXORA Team",
                req.getPublisher() != null ? req.getPublisher() : "NEXORA Team",
                req.isMultiplayer(), req.getMinPlayers(), req.getMaxPlayers(),
                genre, req.getReleaseDate());

        if (req.getPlatformName() != null && !req.getPlatformName().isBlank()) {
            Platform plat = platformRepository.findByName(req.getPlatformName())
                    .orElseGet(() -> platformRepository.save(new Platform(req.getPlatformName())));
            game.addPlatform(plat);
        }

        Game saved = gameRepository.save(game);
        if (req.getScreenshotUrl() != null && !req.getScreenshotUrl().isBlank()) {
            mediaRepository.save(new GameMedia(saved, GameMedia.MediaType.SCREENSHOT, req.getScreenshotUrl()));
        }

        return GameDetailDto.from(saved, mediaRepository.findByGameId(saved.getId()));
    }

    @Transactional
    public GameDetailDto updateGame(Long id, CreateGameRequest req) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + id));

        if (req.getTitle() != null) game.setTitle(req.getTitle());
        if (req.getDescription() != null) game.setDescription(req.getDescription());
        if (req.getDeveloper() != null) game.setDeveloper(req.getDeveloper());
        if (req.getPublisher() != null) game.setPublisher(req.getPublisher());
        if (req.getMinPlayers() != null) game.setMinPlayers(req.getMinPlayers());
        if (req.getMaxPlayers() != null) game.setMaxPlayers(req.getMaxPlayers());
        game.setMultiplayer(req.isMultiplayer());

        if (req.getGenreName() != null && !req.getGenreName().isBlank()) {
            Genre genre = genreRepository.findByName(req.getGenreName())
                    .orElseGet(() -> genreRepository.save(new Genre(req.getGenreName())));
            game.setGenre(genre);
        }

        Game saved = gameRepository.save(game);
        return GameDetailDto.from(saved, mediaRepository.findByGameId(saved.getId()));
    }

    @Transactional
    public void deleteGame(Long id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + id));
        mediaRepository.deleteAll(mediaRepository.findByGameId(id));
        gameRepository.delete(game);
    }
}
