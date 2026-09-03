package org.amalitech.common.logging;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ResponseEntity<List<LogEntry>> list(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(logService.list(limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LogEntry> get(@PathVariable String id) {
        return logService.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<LogEntry>> recent() {
        return ResponseEntity.ok(logService.list(50));
    }

    @DeleteMapping
    public ResponseEntity<Void> clear() {
        logService.clear();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean removed = logService.delete(id);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
