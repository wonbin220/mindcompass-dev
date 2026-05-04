import json
import os

files = {
    "baseline": "/home/wonbin220/mindcompass-dev/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_relabel_weighted_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json",
    "anchor_best": "/home/wonbin220/mindcompass-dev/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_happy_to_calm_penalty02_best_model_balanced_guard_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json",
    "anchor_v1": "/home/wonbin220/mindcompass-dev/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_happy_to_calm_penalty02_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json",
    "parent_v3": "/home/wonbin220/mindcompass-dev/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_margin_loss_v3_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json",
    "child_v3": "/home/wonbin220/mindcompass-dev/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_margin_loss_v3_best_model_balanced_guard_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json",
    "parent_v5": "/home/wonbin220/mindcompass-dev/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_margin_loss_v5_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json",
    "child_v5": "/home/wonbin220/mindcompass-dev/ai-api-fastapi/training/emotion_classifier/artifacts/evaluation/valid_metrics_cpu_compare_medium_manual_seed_v2_fixed_compare_happy_calm_guard_metric_label_smoothing_hidden_dropout20_margin_loss_v5_best_model_balanced_guard_v1_on_manual_seed_v3_happy_to_calm_reviewed_narrow15_fixed_compare.json"
}

results = {}
for name, path in files.items():
    if os.path.exists(path):
        with open(path, "r") as f:
            data = json.load(f)
            focused = data.get("focused_metrics", {})
            results[name] = {
                "f1": focused.get("happy_calm_macro_f1"),
                "h2c_rate": focused.get("happy_to_calm_rate"),
                "c2h_rate": focused.get("calm_to_happy_rate"),
                "acc": data.get("classification_report", {}).get("accuracy")
            }
    else:
        results[name] = "MISSING"

print(json.dumps(results, indent=2))
