import streamlit as st
import pandas as pd
import numpy as np
import pickle
import json
import plotly.graph_objects as go
from pathlib import Path

# --- PAGE CONFIG ---
st.set_page_config(
    page_title="METABRIC Recurrence Predictor",
    page_icon="🧬",
    layout="wide"
)

# --- DIRECTORIES ---
BASE_DIR = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIR / "trained_model" / "perfect_model.pkl"
META_PATH = BASE_DIR / "trained_model" / "model_metadata.json"
STATS_PATH = BASE_DIR / "dataset_stats.json"

# --- CACHED LOADERS ---
@st.cache_resource
def load_model():
    with open(MODEL_PATH, "rb") as f:
        return pickle.load(f)

@st.cache_data
def load_meta():
    with open(META_PATH, "r") as f:
        return json.load(f)

@st.cache_data
def load_stats():
    with open(STATS_PATH, "r") as f:
        return json.load(f)

# --- UI HELPERS ---
def create_gauge(probability, threshold):
    fig = go.Figure(go.Indicator(
        mode="gauge+number",
        value=probability * 100,
        domain={'x': [0, 1], 'y': [0, 1]},
        title={'text': "Recurrence Risk (%)", 'font': {'size': 24}},
        gauge={
            'axis': {'range': [0, 100], 'tickwidth': 1, 'tickcolor': "darkblue"},
            'bar': {'color': "black"},
            'bgcolor': "white",
            'borderwidth': 2,
            'bordercolor': "gray",
            'steps': [
                {'range': [0, threshold * 100], 'color': 'rgba(0, 255, 0, 0.3)'},
                {'range': [threshold * 100, 100], 'color': 'rgba(255, 0, 0, 0.3)'}
            ],
            'threshold': {
                'line': {'color': "red", 'width': 4},
                'thickness': 0.75,
                'value': threshold * 100
            }
        }
    ))
    fig.update_layout(height=350, margin=dict(l=20, r=20, t=50, b=20))
    return fig

# --- MAIN APP ---
def main():
    st.title("🧬 METABRIC Breast Cancer Recurrence Predictor")
    st.markdown("""
    This application uses a calibrated **CatBoost Classifier** (v5.0.0) trained on the METABRIC dataset 
    to estimate the risk of cancer recurrence.
    """)

    # Load resources
    try:
        model = load_model()
        meta = load_meta()
        stats = load_stats()
    except Exception as e:
        st.error(f"Error loading model resources: {e}")
        return

    features_ordered = meta["features"]
    threshold = meta["optimized_threshold"]

    # --- SIDEBAR: SAMPLE CASES ---
    with st.sidebar:
        st.header("Quick test")
        
        def load_case(case_data):
            for k, v in case_data.items():
                st.session_state[k] = v

        if st.button("🔴 Typical High Risk"):
            load_case({
                'age': 45.0, 'tumor_size': 50.0, 'tumor_stage': 3.0, 'grade': 3.0,
                'nodes': 10.0, 'chemo': "Yes", 'er': "Negative", 'her2': "Positive",
                'surgery': "Mastectomy", 'radio': "Yes"
            })
        
        if st.button("🟢 Typical Low Risk"):
            load_case({
                'age': 65.0, 'tumor_size': 15.0, 'tumor_stage': 1.0, 'grade': 1.0,
                'nodes': 0.0, 'chemo': "No", 'er': "Positive", 'her2': "Negative",
                'surgery': "Breast Conserving", 'radio': "Yes"
            })

        st.divider()
        st.subheader("Extra Cases")

        if st.button("🧬 Triple Negative (High)"):
            load_case({
                'age': 40.0, 'tumor_size': 35.0, 'tumor_stage': 2.0, 'grade': 3.0,
                'nodes': 2.0, 'chemo': "Yes", 'er': "Negative", 'her2': "Negative",
                'surgery': "Mastectomy", 'subtype': "Basal"
            })

        if st.button("🌸 Luminal A (Low)"):
            load_case({
                'age': 70.0, 'tumor_size': 12.0, 'tumor_stage': 1.0, 'grade': 1.0,
                'nodes': 0.0, 'chemo': "No", 'er': "Positive", 'her2': "Negative",
                'surgery': "Breast Conserving", 'subtype': "LumA"
            })

        if st.button("⚡ HER2 Positive (High)"):
            load_case({
                'age': 55.0, 'tumor_size': 40.0, 'tumor_stage': 3.0, 'grade': 3.0,
                'nodes': 5.0, 'chemo': "Yes", 'er': "Negative", 'her2': "Positive",
                'surgery': "Mastectomy", 'subtype': "Her2"
            })

        if st.button("👶 Young Patient (Mod)"):
            load_case({
                'age': 32.0, 'tumor_size': 22.0, 'tumor_stage': 2.0, 'grade': 2.0,
                'nodes': 1.0, 'chemo': "No", 'er': "Positive", 'her2': "Negative",
                'surgery': "Breast Conserving", 'subtype': "LumB"
            })

        if st.button("⚠️ Advanced Stage (Critical)"):
            load_case({
                'age': 58.0, 'tumor_size': 85.0, 'tumor_stage': 4.0, 'grade': 3.0,
                'nodes': 25.0, 'chemo': "Yes", 'er': "Positive", 'her2': "Positive",
                'surgery': "Mastectomy", 'subtype': "LumB"
            })

    # --- INPUT FORM ---
    st.subheader("Patient & Tumor Data")
    
    col1, col2, col3 = st.columns(3)
    
    inputs = {}

    with col1:
        st.info("📊 Clinical Info")
        inputs["age_at_diagnosis"] = st.slider("Age at Diagnosis", 20.0, 100.0, st.session_state.get('age', 60.0))
        
        # Helper for selectbox index
        def get_idx(col, key, default):
            val = st.session_state.get(key, default)
            return stats[col].index(val) if val in stats[col] else 0

        inputs["type_of_breast_surgery"] = st.selectbox("Surgery Type", stats["type_of_breast_surgery"], index=get_idx("type_of_breast_surgery", "surgery", "Mastectomy"))
        inputs["chemotherapy"] = st.selectbox("Chemotherapy", stats["chemotherapy"], index=get_idx("chemotherapy", "chemo", "No"))
        inputs["hormone_therapy"] = st.selectbox("Hormone Therapy", stats["hormone_therapy"], index=get_idx("hormone_therapy", "hormone", "No"))
        inputs["radio_therapy"] = st.selectbox("Radio Therapy", stats["radio_therapy"], index=get_idx("radio_therapy", "radio", "No"))
        inputs["inferred_menopausal_state"] = st.selectbox("Menopausal State", stats["inferred_menopausal_state"])

    with col2:
        st.info("🔬 Tumor Profile")
        inputs["tumor_size"] = st.number_input("Tumor Size (mm)", 0.0, 200.0, st.session_state.get('tumor_size', 25.0))
        inputs["tumor_stage"] = st.slider("Tumor Stage", 0.0, 4.0, st.session_state.get('tumor_stage', 2.0))
        inputs["neoplasm_histologic_grade"] = st.slider("Histologic Grade", 1.0, 3.0, st.session_state.get('grade', 2.0))
        inputs["cellularity"] = st.selectbox("Cellularity", stats["cellularity"])
        inputs["primary_tumor_laterality"] = st.selectbox("Laterality", stats["primary_tumor_laterality"])
        inputs["mutation_count"] = st.number_input("Mutation Count", 0.0, 100.0, 5.0)
        inputs["nottingham_prognostic_index"] = st.number_input("Nottingham Index", 0.0, 10.0, 4.0)
        inputs["lymph_nodes_examined_positive"] = st.number_input("Positive Lymph Nodes", 0.0, 50.0, st.session_state.get('nodes', 0.0))

    with col3:
        st.info("🧬 Molecular markers")
        inputs["pam50_claudin_low_subtype"] = st.selectbox("PAM50 Subtype", stats["pam50_claudin_low_subtype"], index=get_idx("pam50_claudin_low_subtype", "subtype", "LumA"))
        inputs["er_status"] = st.selectbox("ER Status", stats["er_status"], index=get_idx("er_status", "er", "Positive"))
        inputs["er_status_measured_by_ihc"] = st.selectbox("ER IHC", stats["er_status_measured_by_ihc"])
        inputs["pr_status"] = st.selectbox("PR Status", stats["pr_status"])
        inputs["her2_status"] = st.selectbox("HER2 Status", stats["her2_status"], index=get_idx("her2_status", "her2", "Negative"))
        inputs["her2_status_measured_by_snp6"] = st.selectbox("HER2 SNP6", stats["her2_status_measured_by_snp6"])
        inputs["3_gene_classifier_subtype"] = st.selectbox("3-Gene Subtype", stats["3_gene_classifier_subtype"])
        inputs["integrative_cluster"] = st.selectbox("Integrative Cluster", stats["integrative_cluster"])
        inputs["tumor_other_histologic_subtype"] = st.selectbox("Histologic Subtype", stats["tumor_other_histologic_subtype"])

    # --- PREDICTION ---
    st.divider()
    
    # Prepare DataFrame in correct order
    input_df = pd.DataFrame([inputs])[features_ordered]
    
    if st.button("🚀 Predict Recurrence Risk", type="primary"):
        probas = model.predict_proba(input_df)[0, 1]
        is_recurrence = probas >= threshold
        
        c1, c2 = st.columns([1, 1])
        
        with c1:
            st.plotly_chart(create_gauge(probas, threshold), use_container_width=True)
        
        with c2:
            st.write("### Prediction Results")
            if is_recurrence:
                st.error(f"**HIGH RISK OF RECURRENCE** (Threshold: {threshold:.2f})")
                st.markdown("The predicted probability exceeds the optimized decision threshold.")
            else:
                st.success(f"**LOW RISK OF RECURRENCE** (Threshold: {threshold:.2f})")
                st.markdown("The predicted probability is below the optimized decision threshold.")
            
            st.metric("Probability", f"{probas*100:.1f}%")
            
            # Additional Info
            with st.expander("Model Technical details"):
                st.write(f"**Model Version:** {meta['version']}")
                st.write(f"**Decision Threshold:** {threshold:.4f}")
                st.write(f"**Test AUC:** {meta['metrics_test']['auc']:.4f}")
                st.write(f"**Test F1-Score:** {meta['metrics_test']['f1']:.4f}")

    # --- ABOUT ---
    st.divider()
    st.caption("Disclaimer: This tool is for research purposes only and should not be used for clinical decision-making.")

if __name__ == "__main__":
    main()
