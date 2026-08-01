import argparse
from collections import defaultdict

import numpy as np
import pandas as pd


COLUMNS = [
    "class",
    "age",
    "menopause",
    "tumor-size",
    "inv-nodes",
    "node-caps",
    "deg-malig",
    "breast",
    "breast-quad",
    "irradiat",
]

AGE_ORDER = [
    "10-19",
    "20-29",
    "30-39",
    "40-49",
    "50-59",
    "60-69",
    "70-79",
    "80-89",
    "90-99",
]

TUMOR_SIZE_ORDER = [
    "0-4",
    "5-9",
    "10-14",
    "15-19",
    "20-24",
    "25-29",
    "30-34",
    "35-39",
    "40-44",
    "45-49",
    "50-54",
    "55-59",
]

INV_NODES_ORDER = [
    "0-2",
    "3-5",
    "6-8",
    "9-11",
    "12-14",
    "15-17",
    "18-20",
    "21-23",
    "24-26",
    "27-29",
    "30-32",
    "33-35",
    "36-39",
]


def load_dataset(path: str) -> pd.DataFrame:
    """Load the breast cancer dataset with expected column names."""
    df = pd.read_csv(path, header=None, names=COLUMNS)

    # If a header row accidentally exists in the CSV, remove it safely.
    if str(df.iloc[0]["class"]).strip().lower() == "class":
        df = df.iloc[1:].reset_index(drop=True)

    for col in COLUMNS:
        df[col] = df[col].astype(str).str.strip()

    return df


def split_range_label(label: str) -> tuple[int, int]:
    low, high = label.split("-")
    return int(low), int(high)


def neighbor_choice(
    current: str,
    ordered_labels: list[str],
    allowed_values: set[str],
    rng: np.random.Generator,
) -> str:
    """Pick current label or an adjacent label, constrained to class-allowed values."""
    if current not in ordered_labels:
        return current

    idx = ordered_labels.index(current)
    candidates = [current]
    weights = [0.60]

    if idx - 1 >= 0:
        left = ordered_labels[idx - 1]
        if left in allowed_values:
            candidates.append(left)
            weights.append(0.20)

    if idx + 1 < len(ordered_labels):
        right = ordered_labels[idx + 1]
        if right in allowed_values:
            candidates.append(right)
            weights.append(0.20)

    total = float(sum(weights))
    probs = [w / total for w in weights]
    return str(rng.choice(candidates, p=probs))


def menopause_age_compatible(menopause: str, age_label: str) -> bool:
    """Enforce medically plausible menopause and age combinations."""
    age_low, _ = split_range_label(age_label)

    if menopause == "lt40":
        return age_low < 40
    if menopause == "ge40":
        return age_low >= 40
    if menopause == "premeno":
        return age_low < 60

    return True


def breast_quad_compatible(breast: str, breast_quad: str) -> bool:
    """Ensure breast quadrant side is coherent with breast side."""
    if breast_quad == "central":
        return True
    if breast == "left":
        return breast_quad.startswith("left")
    if breast == "right":
        return breast_quad.startswith("right")
    return False


def build_class_pair_sets(df: pd.DataFrame, pairs: list[tuple[str, str]]) -> dict[tuple[str, tuple[str, str]], set[tuple[str, str]]]:
    """Store observed pair combinations per class to keep generated rows realistic."""
    pair_sets: dict[tuple[str, tuple[str, str]], set[tuple[str, str]]] = {}
    for cls, class_df in df.groupby("class"):
        for pair in pairs:
            pair_sets[(cls, pair)] = set(class_df[list(pair)].itertuples(index=False, name=None))
    return pair_sets


def row_is_valid(
    row: dict,
    strict: bool,
    pair_sets: dict[tuple[str, tuple[str, str]], set[tuple[str, str]]],
) -> bool:
    """Validate medical compatibility and optionally enforce observed class-wise pairs."""
    cls = row["class"]

    if not menopause_age_compatible(row["menopause"], row["age"]):
        return False

    if not breast_quad_compatible(row["breast"], row["breast-quad"]):
        return False

    if strict:
        checked_pairs = [
            ("inv-nodes", "node-caps"),
            ("deg-malig", "tumor-size"),
            ("tumor-size", "inv-nodes"),
            ("age", "menopause"),
        ]
        for pair in checked_pairs:
            if (row[pair[0]], row[pair[1]]) not in pair_sets[(cls, pair)]:
                return False

    return True


def generate_candidate_row(
    class_df: pd.DataFrame,
    class_allowed_values: dict[str, set[str]],
    rng: np.random.Generator,
) -> dict:
    """Create one candidate by class-preserving permutation plus local range shifts."""
    anchor = class_df.sample(n=1, random_state=int(rng.integers(0, 2**31 - 1))).iloc[0]
    donor = class_df.sample(n=1, random_state=int(rng.integers(0, 2**31 - 1))).iloc[0]

    candidate = anchor.to_dict()

    # Realistic intra-class permutations on categorical features.
    permutable_cols = [
        "menopause",
        "inv-nodes",
        "node-caps",
        "deg-malig",
        "breast",
        "breast-quad",
        "irradiat",
    ]
    for col in permutable_cols:
        if rng.random() < 0.45:
            candidate[col] = donor[col]

    # Controlled local perturbations for ordinal ranges.
    candidate["age"] = neighbor_choice(
        candidate["age"], AGE_ORDER, class_allowed_values["age"], rng
    )
    candidate["tumor-size"] = neighbor_choice(
        candidate["tumor-size"], TUMOR_SIZE_ORDER, class_allowed_values["tumor-size"], rng
    )

    # Optional slight variation on invaded nodes with adjacent classes only.
    if rng.random() < 0.30:
        candidate["inv-nodes"] = neighbor_choice(
            candidate["inv-nodes"],
            INV_NODES_ORDER,
            class_allowed_values["inv-nodes"],
            rng,
        )

    return candidate


def compute_target_counts(df: pd.DataFrame, target_total: int) -> pd.Series:
    """Compute target class counts that preserve original class proportions."""
    original_counts = df["class"].value_counts().sort_index()
    expected = (original_counts / len(df) * target_total).round().astype(int)

    diff = target_total - int(expected.sum())
    if diff != 0:
        major_class = original_counts.idxmax()
        expected.loc[major_class] += diff

    # Never reduce any original class count.
    expected = pd.Series(
        {
            cls: max(int(expected[cls]), int(original_counts[cls]))
            for cls in original_counts.index
        }
    )

    return expected


def augment_dataset(
    df: pd.DataFrame,
    target_total: int = 800,
    seed: int = 42,
    max_attempts_per_class: int = 120000,
) -> pd.DataFrame:
    """Generate synthetic rows while preserving class balance and realism constraints."""
    rng = np.random.default_rng(seed)

    class_target_counts = compute_target_counts(df, target_total)
    current_counts = df["class"].value_counts().sort_index()
    needed_by_class = (class_target_counts - current_counts).astype(int)

    existing_rows = set(df[COLUMNS].itertuples(index=False, name=None))

    pair_definitions = [
        ("inv-nodes", "node-caps"),
        ("deg-malig", "tumor-size"),
        ("tumor-size", "inv-nodes"),
        ("age", "menopause"),
    ]
    pair_sets = build_class_pair_sets(df, pair_definitions)

    generated_rows = []

    for cls, need_count in needed_by_class.items():
        if need_count <= 0:
            continue

        class_df = df[df["class"] == cls].reset_index(drop=True)
        class_allowed_values = {
            col: set(class_df[col].unique())
            for col in ["age", "tumor-size", "inv-nodes"]
        }

        added = 0
        attempts = 0

        # Pass 1: strict compatibility using observed pair constraints.
        while added < need_count and attempts < max_attempts_per_class:
            attempts += 1
            candidate = generate_candidate_row(class_df, class_allowed_values, rng)

            if not row_is_valid(candidate, strict=True, pair_sets=pair_sets):
                continue

            row_key = tuple(candidate[col] for col in COLUMNS)
            if row_key in existing_rows:
                continue

            existing_rows.add(row_key)
            generated_rows.append(candidate)
            added += 1

        # Pass 2: relaxed constraints if strict pass cannot reach target.
        while added < need_count and attempts < (2 * max_attempts_per_class):
            attempts += 1
            candidate = generate_candidate_row(class_df, class_allowed_values, rng)

            if not row_is_valid(candidate, strict=False, pair_sets=pair_sets):
                continue

            row_key = tuple(candidate[col] for col in COLUMNS)
            if row_key in existing_rows:
                continue

            existing_rows.add(row_key)
            generated_rows.append(candidate)
            added += 1

        if added < need_count:
            print(
                f"Warning: class '{cls}' requested {need_count} synthetic rows but created {added}."
            )

    synthetic_df = pd.DataFrame(generated_rows, columns=COLUMNS)
    augmented_df = pd.concat([df.copy(), synthetic_df], ignore_index=True)

    return augmented_df


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Augment breast cancer recurrence dataset with realistic synthetic tabular rows."
    )
    parser.add_argument(
        "--input",
        type=str,
        default="breast-cancer.data",
        help="Path to input CSV file.",
    )
    parser.add_argument(
        "--output",
        type=str,
        default="augmented_breast_cancer.csv",
        help="Path to output augmented CSV file.",
    )
    parser.add_argument(
        "--target-size",
        type=int,
        default=800,
        help="Approximate total number of rows after augmentation.",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=42,
        help="Random seed for reproducible augmentation.",
    )

    args = parser.parse_args()

    original_df = load_dataset(args.input)
    augmented_df = augment_dataset(
        original_df,
        target_total=args.target_size,
        seed=args.seed,
    )

    augmented_df.to_csv(args.output, index=False)

    print(f"Original dataset size: {len(original_df)}")
    print(f"Augmented dataset size: {len(augmented_df)}")

    print("\nClass distribution before augmentation:")
    print(original_df["class"].value_counts().sort_index())

    print("\nClass distribution after augmentation:")
    print(augmented_df["class"].value_counts().sort_index())


if __name__ == "__main__":
    main()
